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
 * Processes LLMResponseSignal on LLMFallbackNeuron.
 * A successful response means the LLM is healthy — records success with the
 * circuit breaker and forwards the signal downstream unchanged.
 */
public class LLMFallbackResponseProcessor implements ISignalProcessor<LLMResponseSignal, LLMFallbackNeuron> {

    private static final Logger logger = LogManager.getLogger(LLMFallbackResponseProcessor.class);
    private static final String DESCRIPTION = "Records LLM success and resets circuit breaker on healthy response";

    private final LLMFallbackNeuron fallbackNeuron;

    public LLMFallbackResponseProcessor(LLMFallbackNeuron fallbackNeuron) {
        this.fallbackNeuron = fallbackNeuron;
    }

    @Override
    public <I extends ISignal> List<I> process(LLMResponseSignal input, LLMFallbackNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || input.getValue() == null) {
            logger.warn("LLMFallbackResponseProcessor received null signal or payload");
            return out;
        }
        logger.debug("LLM healthy response received queryId={}", input.getValue().getQueryId());
        fallbackNeuron.recordSuccess();
        @SuppressWarnings("unchecked")
        I copy = (I) input.copySignal();
        out.add(copy);
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
        return LLMFallbackResponseProcessor.class;
    }

    @Override
    public Class<LLMFallbackNeuron> getNeuronClass() {
        return LLMFallbackNeuron.class;
    }

    @Override
    public Class<LLMResponseSignal> getSignalClass() {
        return LLMResponseSignal.class;
    }
}
