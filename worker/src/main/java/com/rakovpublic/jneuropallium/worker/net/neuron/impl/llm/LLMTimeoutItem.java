/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

/**
 * Payload for LLMTimeoutSignal.
 * Triggers fallback to internal knowledge when the LLM endpoint is unavailable or too slow.
 * Fast loop (epoch 1) so the fallback is initiated without delay.
 */
public class LLMTimeoutItem {

    private String queryId;
    private long elapsedMs;
    private String reason;

    public LLMTimeoutItem() {
    }

    public LLMTimeoutItem(String queryId, long elapsedMs, String reason) {
        this.queryId = queryId;
        this.elapsedMs = elapsedMs;
        this.reason = reason;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
