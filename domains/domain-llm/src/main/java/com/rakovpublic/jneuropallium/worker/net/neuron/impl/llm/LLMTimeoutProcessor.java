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
 * Processes LLMTimeoutSignal on LLMFallbackNeuron.
 * Records the failure with the circuit breaker and returns the timeout
 * signal downstream so that consumers can activate fallback logic.
 */
public class LLMTimeoutProcessor implements ISignalProcessor<LLMTimeoutSignal, LLMFallbackNeuron> {

    private static final Logger logger = LogManager.getLogger(LLMTimeoutProcessor.class);
    private static final String DESCRIPTION = "Records LLM timeout and updates circuit breaker state";

    private final LLMFallbackNeuron fallbackNeuron;

    public LLMTimeoutProcessor(LLMFallbackNeuron fallbackNeuron) {
        this.fallbackNeuron = fallbackNeuron;
    }

    @Override
    public <I extends ISignal> List<I> process(LLMTimeoutSignal input, LLMFallbackNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || input.getValue() == null) {
            logger.warn("LLMTimeoutProcessor received null signal or payload");
            return out;
        }
        logger.warn("LLM timeout recorded queryId={} elapsed={}ms reason={}",
                input.getValue().getQueryId(),
                input.getValue().getElapsedMs(),
                input.getValue().getReason());
        fallbackNeuron.recordFailure();
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
        return LLMTimeoutProcessor.class;
    }

    @Override
    public Class<LLMFallbackNeuron> getNeuronClass() {
        return LLMFallbackNeuron.class;
    }

    @Override
    public Class<LLMTimeoutSignal> getSignalClass() {
        return LLMTimeoutSignal.class;
    }
}
