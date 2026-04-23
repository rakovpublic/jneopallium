/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IContentSelectionNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ContentRecommendationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards an external
 * {@link ContentRecommendationSignal} to an {@link IContentSelectionNeuron}
 * so its softmax argmax remains aligned with upstream ZPD planning.
 */
public class ContentRecommendationProcessor implements ISignalProcessor<ContentRecommendationSignal, IContentSelectionNeuron> {

    private static final String DESCRIPTION = "Aligns content selection with ZPD recommendation";

    @Override
    public <I extends ISignal> List<I> process(ContentRecommendationSignal input, IContentSelectionNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ContentRecommendationProcessor.class; }
    @Override public Class<IContentSelectionNeuron> getNeuronClass() { return IContentSelectionNeuron.class; }
    @Override public Class<ContentRecommendationSignal> getSignalClass() { return ContentRecommendationSignal.class; }
}
