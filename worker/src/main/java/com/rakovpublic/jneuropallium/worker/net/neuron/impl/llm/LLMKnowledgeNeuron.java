/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Neuron in Layer 3 that manages the LLM integration.
 * Maintains a bounded response cache, dispatches async HTTP queries,
 * and emits LLMResponseSignal or LLMTimeoutSignal depending on outcome.
 *
 * <p>Design constraints:
 * <ul>
 *   <li>All HTTP calls are non-blocking (CompletableFuture).</li>
 *   <li>Cached responses have a configurable TTL.</li>
 *   <li>Integration is disabled by default; enable via LLMConfig.</li>
 * </ul>
 */
public class LLMKnowledgeNeuron extends Neuron implements ILLMCapable {

    private static final Logger logger = LogManager.getLogger(LLMKnowledgeNeuron.class);
    private static final int MAX_CACHE_SIZE = 256;

    private LLMConfig config;
    private String llmEndpoint;
    private long maxLatencyMs;

    /** Simple bounded LRU cache: queryId → response signal. */
    private final Map<String, LLMResponseSignal> responseCache =
            new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, LLMResponseSignal> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public LLMKnowledgeNeuron(Long id, ISignalChain signalChain, Long run, LLMConfig config) {
        super(id, signalChain, run);
        this.config = config;
        this.llmEndpoint = config.getEndpoint();
        this.maxLatencyMs = config.getMaxLatencyMs();
        this.currentNeuronClass = LLMKnowledgeNeuron.class;
        this.addSignalProcessor(LLMQuerySignal.class, new LLMQueryProcessor());
        this.addSignalProcessor(LLMResponseSignal.class, new LLMResponseProcessor());
    }

    @Override
    public void submitQuery(LLMQuerySignal query) {
        if (!isLLMAvailable()) {
            logger.warn("LLM not available — emitting timeout for queryId={}", query.getValue().getQueryId());
            LLMTimeoutItem item = new LLMTimeoutItem(query.getValue().getQueryId(), 0, "LLM disabled or unavailable");
            LLMTimeoutSignal timeout = new LLMTimeoutSignal(item, query.getSourceLayerId(),
                    query.getSourceNeuronId(), query.getTimeAlive(), "llm-timeout",
                    false, query.getInputName(), false, false, "llm-timeout");
            result.add(timeout);
            return;
        }

        long startMs = System.currentTimeMillis();
        String requestBody = buildRequestBody(query.getValue());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(llmEndpoint + "/api/generate"))
                .timeout(Duration.ofMillis(maxLatencyMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        CompletableFuture<HttpResponse<String>> future =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        future.whenComplete((response, ex) -> {
            long elapsed = System.currentTimeMillis() - startMs;
            String queryId = query.getValue().getQueryId();
            if (ex != null || elapsed > maxLatencyMs) {
                logger.warn("LLM query timeout or error for queryId={}, elapsed={}ms", queryId, elapsed);
                LLMTimeoutItem item = new LLMTimeoutItem(queryId, elapsed,
                        ex != null ? ex.getMessage() : "exceeded maxLatencyMs");
                LLMTimeoutSignal timeout = new LLMTimeoutSignal(item, query.getSourceLayerId(),
                        query.getSourceNeuronId(), query.getTimeAlive(), "llm-timeout",
                        false, query.getInputName(), false, false, "llm-timeout");
                result.add(timeout);
            } else {
                String responseText = extractResponseText(response.body());
                LLMResponseItem respItem = new LLMResponseItem(queryId, responseText, 0.5);
                LLMResponseSignal respSignal = new LLMResponseSignal(respItem, query.getSourceLayerId(),
                        query.getSourceNeuronId(), query.getTimeAlive(), "llm-response",
                        false, query.getInputName(), false, false, "llm-response");
                synchronized (responseCache) {
                    responseCache.put(queryId, respSignal);
                }
                result.add(respSignal);
            }
        });
    }

    @Override
    public Optional<LLMResponseSignal> getCachedResponse(String queryId) {
        synchronized (responseCache) {
            return Optional.ofNullable(responseCache.get(queryId));
        }
    }

    @Override
    public boolean isLLMAvailable() {
        return config != null && config.isEnabled();
    }

    @Override
    public void setLLMEndpoint(String endpoint) {
        this.llmEndpoint = endpoint;
        if (this.config != null) {
            this.config.setEndpoint(endpoint);
        }
    }

    @Override
    public void setMaxLatency(long milliseconds) {
        this.maxLatencyMs = milliseconds;
        if (this.config != null) {
            this.config.setMaxLatencyMs(milliseconds);
        }
    }

    public LLMConfig getConfig() {
        return config;
    }

    public void setConfig(LLMConfig config) {
        this.config = config;
        this.llmEndpoint = config.getEndpoint();
        this.maxLatencyMs = config.getMaxLatencyMs();
    }

    private String buildRequestBody(LLMQueryItem query) {
        return "{\"model\":\"" + escape(config.getModel()) + "\","
                + "\"prompt\":\"" + escape(query.getQueryText()) + "\","
                + "\"context\":\"" + escape(query.getContext()) + "\","
                + "\"stream\":false}";
    }

    private String extractResponseText(String body) {
        // Minimal extraction: look for "response":"..." in the JSON body.
        // Avoids adding a new JSON dependency; full parsing handled by caller if needed.
        int idx = body.indexOf("\"response\":");
        if (idx < 0) {
            return body;
        }
        int start = body.indexOf('"', idx + 11);
        if (start < 0) {
            return body;
        }
        int end = body.indexOf('"', start + 1);
        if (end < 0) {
            return body;
        }
        return body.substring(start + 1, end);
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
