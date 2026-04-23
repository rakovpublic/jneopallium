/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IConceptMasteryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ItemPresentationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: refocuses an {@link IConceptMasteryNeuron} on the
 * concept carried by an arriving {@link ItemPresentationSignal}.
 */
public class ItemPresentationProcessor implements ISignalProcessor<ItemPresentationSignal, IConceptMasteryNeuron> {

    private static final String DESCRIPTION = "Refocuses concept-mastery on the presented item's concept";

    @Override
    public <I extends ISignal> List<I> process(ItemPresentationSignal input, IConceptMasteryNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ItemPresentationProcessor.class; }
    @Override public Class<IConceptMasteryNeuron> getNeuronClass() { return IConceptMasteryNeuron.class; }
    @Override public Class<ItemPresentationSignal> getSignalClass() { return ItemPresentationSignal.class; }
}
