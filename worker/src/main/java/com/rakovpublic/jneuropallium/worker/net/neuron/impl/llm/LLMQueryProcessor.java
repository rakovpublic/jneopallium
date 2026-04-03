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
 * Processes LLMQuerySignal on ILLMCapable neurons.
 * Dispatches the query asynchronously; the neuron collects the response signals internally.
 * Returns an empty list — results appear later via CompletableFuture.
 */
public class LLMQueryProcessor implements ISignalProcessor<LLMQuerySignal, ILLMCapable> {

    private static final Logger logger = LogManager.getLogger(LLMQueryProcessor.class);
    private static final String DESCRIPTION = "Async LLM query dispatcher — slow loop, epoch 2";

    @Override
    public <I extends ISignal> List<I> process(LLMQuerySignal input, ILLMCapable neuron) {
        if (input == null || input.getValue() == null) {
            logger.warn("LLMQueryProcessor received null signal or payload");
            return new LinkedList<>();
        }
        logger.debug("Dispatching LLM query queryId={} priority={}",
                input.getValue().getQueryId(), input.getValue().getPriority());
        neuron.submitQuery(input);
        return new LinkedList<>();
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
        return LLMQueryProcessor.class;
    }

    @Override
    public Class<ILLMCapable> getNeuronClass() {
        return ILLMCapable.class;
    }

    @Override
    public Class<LLMQuerySignal> getSignalClass() {
        return LLMQuerySignal.class;
    }
}
