/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.adfraud;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudDecision;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudEvent;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud.AdFraudRuntimeScorer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class AdFraudStreamingService implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AdFraudRuntimeScorer scorer;
    private final HttpServer server;
    private final AtomicLong eventsTotal = new AtomicLong();
    private final AtomicLong scoresTotal = new AtomicLong();
    private final AtomicLong modelLoadFailuresTotal = new AtomicLong();
    private final AtomicLong missingFeatureTotal = new AtomicLong();
    private final AtomicLong deduplicatedEventsTotal = new AtomicLong();
    private volatile long lastLatencyNanos;

    public AdFraudStreamingService(int port) throws IOException {
        this(new AdFraudRuntimeScorer(), port);
    }

    public AdFraudStreamingService(AdFraudRuntimeScorer scorer, int port) throws IOException {
        this.scorer = scorer;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", exchange -> json(exchange, 200, Map.of("status", "UP")));
        server.createContext("/ready", exchange -> json(exchange, 200, Map.of(
                "ready", true,
                "modelVerified", scorer.getBundle().isVerified())));
        server.createContext("/v1/model", exchange -> json(exchange, 200, Map.of(
                "modelId", scorer.getBundle().getModelId(),
                "version", scorer.getBundle().getVersion(),
                "schemaVersion", scorer.getBundle().getSchemaVersion(),
                "labels", scorer.getBundle().getLabels())));
        server.createContext("/metrics", this::metrics);
        server.createContext("/v1/ad-fraud/events", this::score);
        server.createContext("/v1/ad-fraud/score", this::score);
        server.createContext("/v1/ad-fraud/feedback", exchange -> json(exchange, 202, Map.of("accepted", true)));
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void score(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            json(exchange, 405, Map.of("error", "POST required"));
            return;
        }
        long start = System.nanoTime();
        eventsTotal.incrementAndGet();
        AdFraudEvent event = MAPPER.readValue(exchange.getRequestBody(), AdFraudEvent.class);
        if (event.eventId == null || event.eventId.isBlank()) missingFeatureTotal.incrementAndGet();
        AdFraudDecision decision = scorer.score(event);
        if (decision.isDuplicateEvent()) deduplicatedEventsTotal.incrementAndGet();
        scoresTotal.incrementAndGet();
        lastLatencyNanos = System.nanoTime() - start;
        json(exchange, 200, decision);
    }

    private void metrics(HttpExchange exchange) throws IOException {
        String body = """
                # TYPE events_total counter
                events_total %d
                # TYPE scores_total counter
                scores_total %d
                # TYPE score_latency_seconds gauge
                score_latency_seconds %.9f
                # TYPE model_load_failures_total counter
                model_load_failures_total %d
                # TYPE missing_feature_total counter
                missing_feature_total %d
                # TYPE deduplicated_events_total counter
                deduplicated_events_total %d
                # TYPE fraud_probability_histogram gauge
                fraud_probability_histogram 0
                # TYPE drift_score gauge
                drift_score 0
                # TYPE review_rate gauge
                review_rate 0
                """.formatted(
                eventsTotal.get(),
                scoresTotal.get(),
                Duration.ofNanos(lastLatencyNanos).toNanos() / 1_000_000_000.0,
                modelLoadFailuresTotal.get(),
                missingFeatureTotal.get(),
                deduplicatedEventsTotal.get());
        bytes(exchange, 200, "text/plain; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    private static void json(HttpExchange exchange, int status, Object payload) throws IOException {
        bytes(exchange, status, "application/json; charset=utf-8", MAPPER.writeValueAsBytes(payload));
    }

    private static void bytes(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8088;
        AdFraudStreamingService service = new AdFraudStreamingService(port);
        service.start();
        System.out.println("ad-fraud service listening on " + service.port());
    }
}
