/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

/**
 * Payload for LLMConfidenceSignal.
 * Produced by LLMVerificationNeuron after cross-validation.
 * Carries the verified confidence score and an applicability verdict.
 */
public class LLMConfidenceItem {

    private String queryId;
    /** Verified confidence after cross-validation, 0.0–1.0. */
    private double verifiedConfidence;
    /** True when the LLM response is applicable and safe to use. */
    private boolean applicable;
    private String verificationNote;

    public LLMConfidenceItem() {
    }

    public LLMConfidenceItem(String queryId, double verifiedConfidence, boolean applicable, String verificationNote) {
        this.queryId = queryId;
        this.verifiedConfidence = verifiedConfidence;
        this.applicable = applicable;
        this.verificationNote = verificationNote;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public double getVerifiedConfidence() {
        return verifiedConfidence;
    }

    public void setVerifiedConfidence(double verifiedConfidence) {
        this.verifiedConfidence = verifiedConfidence;
    }

    public boolean isApplicable() {
        return applicable;
    }

    public void setApplicable(boolean applicable) {
        this.applicable = applicable;
    }

    public String getVerificationNote() {
        return verificationNote;
    }

    public void setVerificationNote(String verificationNote) {
        this.verificationNote = verificationNote;
    }
}
