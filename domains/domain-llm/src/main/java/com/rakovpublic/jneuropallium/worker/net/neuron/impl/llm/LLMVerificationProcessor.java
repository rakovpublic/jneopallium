/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Internal processor used by LLMVerificationNeuron.
 * Delegates to {@link LLMVerificationNeuron#verify(LLMResponseSignal)} and emits
 * a LLMConfidenceSignal for downstream consumption.
 */
public class LLMVerificationProcessor implements ISignalProcessor<LLMResponseSignal, LLMVerificationNeuron> {

    private static final Logger logger = LogManager.getLogger(LLMVerificationProcessor.class);
    private static final String DESCRIPTION = "Cross-validates raw LLM responses and emits confidence signals";

    private final LLMVerificationNeuron verificationNeuron;

    public LLMVerificationProcessor(LLMVerificationNeuron verificationNeuron) {
        this.verificationNeuron = verificationNeuron;
    }

    @Override
    public <I extends ISignal> List<I> process(LLMResponseSignal input, LLMVerificationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || input.getValue() == null) {
            logger.warn("LLMVerificationProcessor received null signal or payload");
            return out;
        }
        LLMConfidenceSignal confidence = verificationNeuron.verify(input);
        @SuppressWarnings("unchecked")
        I cs = (I) confidence;
        out.add(cs);
        return out;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return LLMVerificationProcessor.class;
    }

    @Override
    public Class<LLMVerificationNeuron> getNeuronClass() {
        return LLMVerificationNeuron.class;
    }

    @Override
    public Class<LLMResponseSignal> getSignalClass() {
        return LLMResponseSignal.class;
    }
}
