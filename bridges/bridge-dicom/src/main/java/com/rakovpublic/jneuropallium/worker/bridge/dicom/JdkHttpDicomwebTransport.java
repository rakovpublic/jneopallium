/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

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
 * Production {@link DicomwebTransport} backed by {@link HttpClient}
 * (07-DICOM.md §3, §4, §10 R1).
 *
 * <p>This transport issues only HTTP {@code GET} requests; it has no
 * method that takes an HTTP verb argument and no path that constructs a
 * request with a body. {@code Accept} is fixed to
 * {@code application/dicom+json} so pixel data is never returned (§10 R1
 * "pixel data path is unreachable").
 *
 * <p>The class is a thin convenience around the JDK's built-in client to
 * avoid pulling the full dcm4che dependency tree as a hard requirement.
 * Deployments that prefer dcm4che's {@code WadoRSClient} may replace this
 * implementation — they must mirror the read-only seam exactly.
 */
public final class JdkHttpDicomwebTransport implements DicomwebTransport {

    private static final Logger log = LoggerFactory.getLogger(JdkHttpDicomwebTransport.class);

    /** Per §10 R1 — pixel data must never enter the JVM heap. */
    public static final String ACCEPT_HEADER = "application/dicom+json";

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final DicomBridgeConfig.SecurityConfig security;
    private final AtomicReference<String> bearerCache = new AtomicReference<>();

    public JdkHttpDicomwebTransport(DicomBridgeConfig config) {
        this(config, HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                new ObjectMapper());
    }

    public JdkHttpDicomwebTransport(DicomBridgeConfig config, HttpClient http, ObjectMapper mapper) {
        Objects.requireNonNull(config, "config");
        if (config.dicomweb() == null) {
            throw new IllegalArgumentException(
                    "JdkHttpDicomwebTransport requires a 'dicomweb:' config block.");
        }
        this.http = Objects.requireNonNull(http, "http");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.baseUrl = stripTrailingSlash(config.dicomweb().baseUrl());
        this.security = config.security() == null
                ? new DicomBridgeConfig.SecurityConfig(
                        DicomBridgeConfig.SecurityType.NONE, null, null, null, null,
                        null, null, null, null)
                : config.security();
    }

    @Override
    public JsonNode qido(String path) throws IOException {
        return getJson(absoluteUrl(path));
    }

    @Override
    public JsonNode wadoMetadata(String path) throws IOException {
        return getJson(absoluteUrl(path));
    }

    @Override
    public boolean isReady() {
        if (security.type() != DicomBridgeConfig.SecurityType.OAUTH2_BEARER_TOKEN) {
            return true;
        }
        return bearerCache.get() != null;
    }

    /** Force the next call to re-authenticate (e.g. after a 401). */
    public void invalidateToken() {
        bearerCache.set(null);
    }

    private JsonNode getJson(String url) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", ACCEPT_HEADER)
                .GET()
                .timeout(Duration.ofSeconds(30));
        applyAuth(b);
        try {
            HttpResponse<byte[]> response = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            int code = response.statusCode();
            if (code == 401 || code == 403) {
                if (security.type() == DicomBridgeConfig.SecurityType.OAUTH2_BEARER_TOKEN) {
                    invalidateToken();
                }
                throw new IOException("DICOMweb GET " + url + " returned " + code);
            }
            if (code == 404) {
                return mapper.createArrayNode();
            }
            if (code >= 400) {
                throw new IOException("DICOMweb GET " + url + " returned " + code);
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                return mapper.createArrayNode();
            }
            return mapper.readTree(body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while issuing DICOMweb GET " + url, e);
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

    private String absoluteUrl(String relative) {
        if (relative == null || relative.isEmpty()) return baseUrl;
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative;
        }
        String rel = relative.startsWith("/") ? relative.substring(1) : relative;
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
