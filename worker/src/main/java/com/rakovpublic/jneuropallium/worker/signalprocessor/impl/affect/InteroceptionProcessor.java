/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AnteriorInsulaNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.IInteroceptive;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that feeds {@link InteroceptiveSignal}s into an
 * interoceptive neuron (typically {@link AnteriorInsulaNeuron}).
 */
public class InteroceptionProcessor implements ISignalProcessor<InteroceptiveSignal, IInteroceptive> {

    private static final String DESCRIPTION = "Integrates interoceptive telemetry into the insula neuron";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(InteroceptiveSignal input, IInteroceptive neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        if (neuron instanceof AnteriorInsulaNeuron) {
            ((AnteriorInsulaNeuron) neuron).integrate(input);
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return InteroceptionProcessor.class; }
    @Override public Class<IInteroceptive> getNeuronClass() { return IInteroceptive.class; }
    @Override public Class<InteroceptiveSignal> getSignalClass() { return InteroceptiveSignal.class; }
}
