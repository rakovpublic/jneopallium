/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.IAffectModulationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that routes an {@link AffectStateSignal} to an
 * {@link IAffectModulationNeuron}, which broadcasts modulation to the
 * rest of the network (learning-rate, harm-threshold, salience).
 */
public class AffectStateModulationProcessor implements ISignalProcessor<AffectStateSignal, IAffectModulationNeuron> {

    private static final String DESCRIPTION = "Broadcasts affect state to modulation neuron";

    @Override
    public <I extends ISignal> List<I> process(AffectStateSignal input, IAffectModulationNeuron neuron) {
        if (input != null && neuron != null) neuron.onAffect(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AffectStateModulationProcessor.class; }
    @Override public Class<IAffectModulationNeuron> getNeuronClass() { return IAffectModulationNeuron.class; }
    @Override public Class<AffectStateSignal> getSignalClass() { return AffectStateSignal.class; }
}
