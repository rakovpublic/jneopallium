/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production {@link FhirTransport} backed by {@link HttpClient}
 * (06-FHIR.md §3 rule 1, §4 architecture diagram).
 *
 * <p>This transport issues only HTTP {@code GET} requests; it has no
 * method that takes an HTTP verb argument and no path that constructs a
 * request with a body. Authentication is bearer-token / basic / none —
 * mTLS is out of scope here and left to a future
 * {@code MtlsHttpFhirTransport} that wraps a configured {@link HttpClient}
 * with the operator's truststore and keystore.
 *
 * <p>The class is a thin convenience around the JDK's built-in client to
 * avoid pulling the full HAPI FHIR dependency tree (06-FHIR.md §10 R5).
 * Deployments that prefer HAPI may replace this implementation with a
 * {@code HapiFhirTransport} that delegates to
 * {@code IGenericClient.read()} / {@code IGenericClient.search()} only.
 */
public final class JdkHttpFhirTransport implements FhirTransport {

    private static final Logger log = LoggerFactory.getLogger(JdkHttpFhirTransport.class);

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final FhirBridgeConfig.SecurityConfig security;
    private final AtomicReference<String> bearerCache = new AtomicReference<>();

    public JdkHttpFhirTransport(FhirBridgeConfig config) {
        this(config, HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    public JdkHttpFhirTransport(FhirBridgeConfig config, HttpClient http, ObjectMapper mapper) {
        Objects.requireNonNull(config, "config");
        this.http = Objects.requireNonNull(http, "http");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.baseUrl = stripTrailingSlash(config.fhir().baseUrl());
        this.security = config.security() == null
                ? new FhirBridgeConfig.SecurityConfig(
                        FhirBridgeConfig.SecurityType.NONE, null, null, null, null,
                        null, null, null, null)
                : config.security();
    }

    @Override
    public JsonNode read(String reference) throws IOException {
        return getJson(absoluteUrl(reference));
    }

    @Override
    public JsonNode search(String searchExpression) throws IOException {
        return getJson(absoluteUrl(searchExpression));
    }

    /**
     * Best-effort token freshness signal. If a bearer is configured but
     * not yet acquired (or has been cleared after a 401), the next
     * {@link #read} / {@link #search} call will trigger a token refresh.
     * Callers can use this to skip a poll cycle gracefully (06-FHIR.md
     * §9 S8).
     */
    @Override
    public boolean isReady() {
        if (security.type() != FhirBridgeConfig.SecurityType.OAUTH2_BEARER_TOKEN) {
            return true;
        }
        return bearerCache.get() != null;
    }

    /** Force the next call to re-authenticate (e.g. after a 401). §9 S8. */
    public void invalidateToken() {
        bearerCache.set(null);
    }

    private JsonNode getJson(String url) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/fhir+json")
                .GET()
                .timeout(Duration.ofSeconds(30));
        applyAuth(b);
        try {
            HttpResponse<byte[]> response = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            int code = response.statusCode();
            if (code == 401 || code == 403) {
                if (security.type() == FhirBridgeConfig.SecurityType.OAUTH2_BEARER_TOKEN) {
                    invalidateToken();
                }
                throw new IOException("FHIR GET " + url + " returned " + code);
            }
            if (code >= 400) {
                throw new IOException("FHIR GET " + url + " returned " + code);
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while issuing FHIR GET " + url, e);
        }
    }

    private void applyAuth(HttpRequest.Builder b) throws IOException {
        switch (security.type()) {
            case OAUTH2_BEARER_TOKEN -> {
                String token = bearerCache.get();
                if (token == null) {
                    token = acquireBearerToken();
                    bearerCache.set(token);
                }
                if (token != null) b.header("Authorization", "Bearer " + token);
            }
            case BASIC_AUTH -> {
                String user = security.basicAuthUserEnv() == null
                        ? null : System.getenv(security.basicAuthUserEnv());
                String pass = security.basicAuthPassEnv() == null
                        ? null : System.getenv(security.basicAuthPassEnv());
                if (user != null && pass != null) {
                    String enc = Base64.getEncoder().encodeToString(
                            (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
                    b.header("Authorization", "Basic " + enc);
                }
            }
            case MUTUAL_TLS, NONE -> { /* no header */ }
        }
    }

    private String acquireBearerToken() throws IOException {
        String endpoint = security.tokenEndpoint();
        String clientId = security.clientId();
        String secretEnv = security.clientSecretEnv();
        String secret = secretEnv == null ? null : System.getenv(secretEnv);
        if (endpoint == null || clientId == null || secret == null) {
            log.warn("OAuth2 not fully configured (tokenEndpoint/clientId/{}=secret); skipping",
                    secretEnv);
            return null;
        }
        StringBuilder form = new StringBuilder();
        form.append("grant_type=client_credentials");
        form.append("&client_id=").append(urlEncode(clientId));
        form.append("&client_secret=").append(urlEncode(secret));
        if (security.scope() != null && !security.scope().isEmpty()) {
            form.append("&scope=").append(urlEncode(security.scope()));
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString(), StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(15))
                .build();
        try {
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new IOException("Token endpoint returned " + resp.statusCode());
            }
            JsonNode node = mapper.readTree(resp.body());
            JsonNode token = node.get("access_token");
            return token == null ? null : token.asText();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while acquiring OAuth2 token", e);
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

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
