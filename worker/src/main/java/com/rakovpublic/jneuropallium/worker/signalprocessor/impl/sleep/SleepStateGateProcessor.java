/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.sleep;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SleepControllerNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.ISleepControllerNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.SleepStateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that propagates the latest {@link SleepStateSignal}
 * to downstream neurons. The controller uses the arrival to advance one
 * cycle tick.
 */
public class SleepStateGateProcessor implements ISignalProcessor<SleepStateSignal, ISleepControllerNeuron> {

    private static final String DESCRIPTION = "Sleep-state gate re-emission";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SleepStateSignal input, ISleepControllerNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.setPhase(input.getPhase());
        SleepStateSignal advanced = neuron.advance();
        if (advanced != null) out.add((I) advanced);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SleepStateGateProcessor.class; }
    @Override public Class<ISleepControllerNeuron> getNeuronClass() { return ISleepControllerNeuron.class; }
    @Override public Class<SleepStateSignal> getSignalClass() { return SleepStateSignal.class; }
}
