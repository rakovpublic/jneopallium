/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Trust boundary neuron (Layer 3).
 * Receives raw LLMResponseSignal, cross-validates content against internal models,
 * and emits LLMConfidenceSignal with a verified confidence score and applicability verdict.
 *
 * <p>Verification strategy:
 * <ol>
 *   <li>Null/empty response → not applicable, confidence=0.</li>
 *   <li>Response length heuristic (too short or suspiciously long) → reduced confidence.</li>
 *   <li>Custom validators registered via {@link #addValidator(LLMResponseValidator)}.</li>
 * </ol>
 *
 * <p>Verified info enters WorkingMemory with a reduced TTL — never LongTermMemory directly.
 */
public class LLMVerificationNeuron extends Neuron implements ILLMVerificationNeuron {

    private static final Logger logger = LogManager.getLogger(LLMVerificationNeuron.class);

    /** Pluggable cross-validation rules. */
    private final List<LLMResponseValidator> validators = new ArrayList<>();

    /** Minimum confidence threshold to consider a response applicable. */
    private double applicabilityThreshold = 0.5;

    public LLMVerificationNeuron(Long id, ISignalChain signalChain, Long run) {
        super(id, signalChain, run);
        this.currentNeuronClass = LLMVerificationNeuron.class;
        this.addSignalProcessor(LLMResponseSignal.class, new LLMVerificationProcessor(this));
    }

    /**
     * Verifies a raw LLM response and produces a LLMConfidenceSignal.
     *
     * @param responseSignal the raw response to validate
     * @return confidence signal with verdict
     */
    public LLMConfidenceSignal verify(LLMResponseSignal responseSignal) {
        LLMResponseItem item = responseSignal.getValue();

        if (item == null || item.getResponseText() == null || item.getResponseText().isBlank()) {
            logger.warn("LLM response is null/blank for queryId={}", item != null ? item.getQueryId() : "unknown");
            return buildConfidenceSignal(responseSignal, 0.0, false, "null or blank response");
        }

        double confidence = item.getRawConfidence();

        // Length heuristic
        int len = item.getResponseText().length();
        if (len < 10) {
            confidence *= 0.3;
        } else if (len > 10_000) {
            confidence *= 0.7;
        }

        // Custom validators
        for (LLMResponseValidator validator : validators) {
            confidence = validator.validate(item, confidence);
        }

        confidence = Math.max(0.0, Math.min(1.0, confidence));
        boolean applicable = confidence >= applicabilityThreshold;

        logger.debug("Verified LLM response queryId={} confidence={} applicable={}",
                item.getQueryId(), confidence, applicable);

        return buildConfidenceSignal(responseSignal, confidence, applicable,
                applicable ? "passed verification" : "below applicability threshold");
    }

    private LLMConfidenceSignal buildConfidenceSignal(LLMResponseSignal source,
                                                       double confidence,
                                                       boolean applicable,
                                                       String note) {
        LLMResponseItem src = source.getValue();
        String queryId = src != null ? src.getQueryId() : "unknown";
        LLMConfidenceItem ci = new LLMConfidenceItem(queryId, confidence, applicable, note);
        return new LLMConfidenceSignal(ci, source.getSourceLayerId(), source.getSourceNeuronId(),
                source.getTimeAlive(), "llm-confidence", false, source.getInputName(),
                false, false, "llm-confidence-" + queryId);
    }

    public void addValidator(LLMResponseValidator validator) {
        validators.add(validator);
    }

    public double getApplicabilityThreshold() {
        return applicabilityThreshold;
    }

    public void setApplicabilityThreshold(double applicabilityThreshold) {
        this.applicabilityThreshold = applicabilityThreshold;
    }

    /**
     * Functional interface for pluggable validation rules.
     */
    @FunctionalInterface
    public interface LLMResponseValidator {
        /**
         * Inspect the response and adjust the confidence score.
         *
         * @param item       the response payload
         * @param confidence current running confidence (0.0–1.0)
         * @return adjusted confidence (must stay in [0.0, 1.0])
         */
        double validate(LLMResponseItem item, double confidence);
    }
}
