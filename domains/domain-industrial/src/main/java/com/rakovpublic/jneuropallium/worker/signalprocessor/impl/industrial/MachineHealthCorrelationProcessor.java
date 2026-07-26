/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IMachineHealthCorrelationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

import java.util.LinkedList;
import java.util.List;

/** Converts fault hypotheses into machine-health advisory signals. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MachineHealthCorrelationProcessor implements ISignalProcessor<FaultHypothesisSignal, IMachineHealthCorrelationNeuron> {

    private static final String DESCRIPTION = "Machine-health temporal correlation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(FaultHypothesisSignal input, IMachineHealthCorrelationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        MachineHealthAdvisorySignal advisory = neuron.correlate(input);
        if (advisory != null) out.add((I) advisory);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MachineHealthCorrelationProcessor.class; }
    @Override public Class<IMachineHealthCorrelationNeuron> getNeuronClass() { return IMachineHealthCorrelationNeuron.class; }
    @Override public Class<FaultHypothesisSignal> getSignalClass() { return FaultHypothesisSignal.class; }
}
