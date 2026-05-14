/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

/**
 * Production {@link XapiTransport} backed by {@link HttpClient}
 * (14-LTI-XAPI.md §4 architecture diagram).
 *
 * <p>Reads (GET statements) and writes (POST statement / POST AGS Score)
 * are the only methods this transport exposes — there is no surface that
 * could auto-enrol a learner or modify the roster.
 */
public final class JdkHttpXapiTransport implements XapiTransport {

    private static final Logger log = LoggerFactory.getLogger(JdkHttpXapiTransport.class);

    /**
     * Mandatory xAPI version header (xAPI 1.0.3 — the version supported by
     * tincan-java and the public LRS implementations referenced by §2).
     */
    public static final String XAPI_VERSION = "1.0.3";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final LtiBridgeConfig.AuthConfig auth;

    public JdkHttpXapiTransport(LtiBridgeConfig config) {
        this(config, HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    public JdkHttpXapiTransport(LtiBridgeConfig config, HttpClient http, ObjectMapper mapper) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(config.xapi(), "config.xapi");
        Objects.requireNonNull(config.xapi().lrs(), "config.xapi.lrs");
        this.http = Objects.requireNonNull(http, "http");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.baseUrl = stripTrailingSlash(config.xapi().lrs().endpoint());
        this.auth = config.xapi().lrs().auth() == null
                ? new LtiBridgeConfig.AuthConfig(LtiBridgeConfig.AuthType.NONE, null, null, null)
                : config.xapi().lrs().auth();
    }

    @Override
    public JsonNode pollStatements(String query) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(absoluteUrl(query)))
                .header("Accept", "application/json")
                .header("X-Experience-API-Version", XAPI_VERSION)
                .GET()
                .timeout(Duration.ofSeconds(30));
        applyAuth(b);
        return send(b.build());
    }

    @Override
    public String postStatement(JsonNode statement) throws IOException {
        byte[] body = mapper.writeValueAsBytes(statement);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(absoluteUrl("statements")))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Experience-API-Version", XAPI_VERSION)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(Duration.ofSeconds(30));
        applyAuth(b);
        JsonNode response = send(b.build());
        if (response.isArray() && response.size() > 0) {
            return response.get(0).asText();
        }
        return null;
    }

    @Override
    public void postAgsScore(String lineItemUrl, JsonNode score) throws IOException {
        byte[] body = mapper.writeValueAsBytes(score);
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(lineItemUrl + "/scores"))
                .header("Accept", "application/vnd.ims.lis.v1.score+json")
                .header("Content-Type", "application/vnd.ims.lis.v1.score+json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(Duration.ofSeconds(30));
        applyAuth(b);
        send(b.build());
    }

    private JsonNode send(HttpRequest req) throws IOException {
        try {
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int code = resp.statusCode();
            if (code >= 400) {
                throw new IOException("xAPI " + req.method() + " "
                        + req.uri() + " returned " + code);
            }
            byte[] body = resp.body();
            if (body == null || body.length == 0) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while issuing xAPI request", e);
        }
    }

    private void applyAuth(HttpRequest.Builder b) {
        switch (auth.type()) {
            case BASIC_AUTH -> {
                String user = auth.usernameEnv() == null
                        ? null : System.getenv(auth.usernameEnv());
                String pass = auth.passwordEnv() == null
                        ? null : System.getenv(auth.passwordEnv());
                if (user != null && pass != null) {
                    String enc = Base64.getEncoder().encodeToString(
                            (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                    b.header("Authorization", "Basic " + enc);
                } else {
                    log.warn("xAPI BasicAuth not configured ({}={}, {}={})",
                            auth.usernameEnv(), user == null ? "null" : "(set)",
                            auth.passwordEnv(), pass == null ? "null" : "(set)");
                }
            }
            case BEARER_TOKEN -> {
                String token = auth.tokenEnv() == null
                        ? null : System.getenv(auth.tokenEnv());
                if (token != null) b.header("Authorization", "Bearer " + token);
            }
            case NONE -> { /* no header */ }
        }
    }

    private String absoluteUrl(String relativeOrAbsolute) {
        if (relativeOrAbsolute == null || relativeOrAbsolute.isEmpty()) return baseUrl;
        if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            return relativeOrAbsolute;
        }
        String rel = relativeOrAbsolute.startsWith("/")
                ? relativeOrAbsolute.substring(1)
                : relativeOrAbsolute;
        return baseUrl + "/" + rel;
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
