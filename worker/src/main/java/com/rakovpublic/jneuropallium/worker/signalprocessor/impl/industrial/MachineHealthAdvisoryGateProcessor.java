/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IAdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

import java.util.LinkedList;
import java.util.List;

/** Applies advisory-only safety semantics to machine-health outputs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineHealthAdvisoryGateProcessor implements ISignalProcessor<MachineHealthAdvisorySignal, IAdvisoryGateNeuron> {

    private static final String DESCRIPTION = "Machine-health advisory gate";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MachineHealthAdvisorySignal input, IAdvisoryGateNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        MachineHealthAdvisorySignal gated = neuron.gate(input);
        if (gated != null) out.add((I) gated);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MachineHealthAdvisoryGateProcessor.class; }
    @Override public Class<IAdvisoryGateNeuron> getNeuronClass() { return IAdvisoryGateNeuron.class; }
    @Override public Class<MachineHealthAdvisorySignal> getSignalClass() { return MachineHealthAdvisorySignal.class; }
}
