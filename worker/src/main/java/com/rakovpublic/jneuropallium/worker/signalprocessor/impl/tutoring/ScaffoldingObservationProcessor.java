/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring.IScaffoldingNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ScaffoldingSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records an external {@link ScaffoldingSignal} on
 * an {@link IScaffoldingNeuron} to avoid duplicating effort.
 */
public class ScaffoldingObservationProcessor implements ISignalProcessor<ScaffoldingSignal, IScaffoldingNeuron> {

    private static final String DESCRIPTION = "Records external scaffolding on the scaffolding neuron";

    @Override
    public <I extends ISignal> List<I> process(ScaffoldingSignal input, IScaffoldingNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ScaffoldingObservationProcessor.class; }
    @Override public Class<IScaffoldingNeuron> getNeuronClass() { return IScaffoldingNeuron.class; }
    @Override public Class<ScaffoldingSignal> getSignalClass() { return ScaffoldingSignal.class; }
}
