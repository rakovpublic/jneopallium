/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IEntityBehaviourBaselineNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.InflammationBroadcastSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: relays an {@link InflammationBroadcastSignal} to
 * the baseline neuron, which freezes adaptation while the region is
 * inflamed. Cytokine-style broadcast.
 */
public class InflammationBaselineProcessor implements ISignalProcessor<InflammationBroadcastSignal, IEntityBehaviourBaselineNeuron> {

    private static final String DESCRIPTION = "Inflammation → baseline-freeze broadcast";

    @Override
    public <I extends ISignal> List<I> process(InflammationBroadcastSignal input, IEntityBehaviourBaselineNeuron neuron) {
        if (input != null && neuron != null) neuron.onInflammation(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return InflammationBaselineProcessor.class; }
    @Override public Class<IEntityBehaviourBaselineNeuron> getNeuronClass() { return IEntityBehaviourBaselineNeuron.class; }
    @Override public Class<InflammationBroadcastSignal> getSignalClass() { return InflammationBroadcastSignal.class; }
}
