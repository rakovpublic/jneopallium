/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

/**
 * Payload for LLMResponseSignal.
 * Carries the raw LLM response, the originating query id, and an initial confidence score.
 * Responses are UNTRUSTED until cross-validated by LLMVerificationNeuron.
 */
public class LLMResponseItem {

    private String queryId;
    private String responseText;
    /** Raw confidence score from 0.0 to 1.0 — not yet verified. */
    private double rawConfidence;

    public LLMResponseItem() {
    }

    public LLMResponseItem(String queryId, String responseText, double rawConfidence) {
        this.queryId = queryId;
        this.responseText = responseText;
        this.rawConfidence = rawConfidence;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public double getRawConfidence() {
        return rawConfidence;
    }

    public void setRawConfidence(double rawConfidence) {
        this.rawConfidence = rawConfidence;
    }
}
