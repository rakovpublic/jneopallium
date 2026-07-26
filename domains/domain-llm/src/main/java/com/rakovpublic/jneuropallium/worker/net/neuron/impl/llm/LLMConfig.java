/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

/**
 * Configuration for the LLM integration module.
 * All fields map to the {@code llm:} section of the jneopallium config file.
 * Default state is disabled — LLM must be explicitly enabled.
 */
public class LLMConfig {

    private boolean enabled = false;
    private String endpoint = "http://localhost:11434";
    private String apiKey = "";
    private long maxLatencyMs = 5000;
    private long cacheTtlSeconds = 300;
    private String model = "llama3";
    private int circuitBreakerFailureThreshold = 5;
    private long circuitBreakerHalfOpenProbeIntervalMs = 30000;

    public LLMConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public long getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public void setMaxLatencyMs(long maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }

    public long getCircuitBreakerHalfOpenProbeIntervalMs() {
        return circuitBreakerHalfOpenProbeIntervalMs;
    }

    public void setCircuitBreakerHalfOpenProbeIntervalMs(long circuitBreakerHalfOpenProbeIntervalMs) {
        this.circuitBreakerHalfOpenProbeIntervalMs = circuitBreakerHalfOpenProbeIntervalMs;
    }
}
