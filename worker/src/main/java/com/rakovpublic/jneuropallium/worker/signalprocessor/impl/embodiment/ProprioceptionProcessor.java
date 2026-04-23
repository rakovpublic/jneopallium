/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IEmbodied;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that forwards {@link ProprioceptiveSignal} into an
 * {@link IEmbodied} neuron so it can update its body-schema snapshot.
 */
public class ProprioceptionProcessor implements ISignalProcessor<ProprioceptiveSignal, IEmbodied> {

    private static final String DESCRIPTION = "Feeds proprioceptive telemetry into the body-schema neuron";

    @Override
    public <I extends ISignal> List<I> process(ProprioceptiveSignal input, IEmbodied neuron) {
        if (input != null && neuron != null) neuron.onProprioceptive(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ProprioceptionProcessor.class; }
    @Override public Class<IEmbodied> getNeuronClass() { return IEmbodied.class; }
    @Override public Class<ProprioceptiveSignal> getSignalClass() { return ProprioceptiveSignal.class; }
}
