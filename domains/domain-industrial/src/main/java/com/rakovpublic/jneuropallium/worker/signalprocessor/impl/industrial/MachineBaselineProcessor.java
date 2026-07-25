/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IMachineBaselineNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;

import java.util.LinkedList;
import java.util.List;

/** Updates machine baselines and emits domain-shift signals. */
public class MachineBaselineProcessor implements ISignalProcessor<MachineFeatureSignal, IMachineBaselineNeuron> {

    private static final String DESCRIPTION = "Machine baseline and domain-shift update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MachineFeatureSignal input, IMachineBaselineNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        DomainShiftSignal shift = neuron.observe(input);
        if (shift != null) out.add((I) shift);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MachineBaselineProcessor.class; }
    @Override public Class<IMachineBaselineNeuron> getNeuronClass() { return IMachineBaselineNeuron.class; }
    @Override public Class<MachineFeatureSignal> getSignalClass() { return MachineFeatureSignal.class; }
}
