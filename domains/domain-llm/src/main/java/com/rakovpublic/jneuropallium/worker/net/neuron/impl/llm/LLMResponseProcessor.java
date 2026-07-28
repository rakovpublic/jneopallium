/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Processes a raw LLMResponseSignal on any INeuron.
 * Passes the response downstream for verification (to LLMVerificationNeuron).
 * Responses enter WorkingMemory with reduced TTL — never LongTermMemory directly.
 *
 * <p>Returning the signal as-is forwards it to the next layer via Axon routing.
 */
public class LLMResponseProcessor implements ISignalProcessor<LLMResponseSignal, INeuron> {

    private static final Logger logger = LogManager.getLogger(LLMResponseProcessor.class);
    private static final String DESCRIPTION = "LLM response forwarder — routes raw response to verification layer";

    @Override
    public <I extends ISignal> List<I> process(LLMResponseSignal input, INeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || input.getValue() == null) {
            logger.warn("LLMResponseProcessor received null signal or payload");
            return out;
        }
        logger.debug("Forwarding LLM response queryId={} rawConfidence={}",
                input.getValue().getQueryId(), input.getValue().getRawConfidence());
        // Pass a copy so upstream state is not mutated during routing
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
        return LLMResponseProcessor.class;
    }

    @Override
    public Class<INeuron> getNeuronClass() {
        return INeuron.class;
    }

    @Override
    public Class<LLMResponseSignal> getSignalClass() {
        return LLMResponseSignal.class;
    }
}
