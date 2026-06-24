/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IFaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;

import java.util.LinkedList;
import java.util.List;

/** Builds fault hypotheses from compressed feature signals. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaultHypothesisProcessor implements ISignalProcessor<MachineFeatureSignal, IFaultHypothesisNeuron> {

    private static final String DESCRIPTION = "Machine fault hypothesis update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MachineFeatureSignal input, IFaultHypothesisNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        FaultHypothesisSignal hypothesis = neuron.hypothesize(input);
        if (hypothesis != null) out.add((I) hypothesis);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FaultHypothesisProcessor.class; }
    @Override public Class<IFaultHypothesisNeuron> getNeuronClass() { return IFaultHypothesisNeuron.class; }
    @Override public Class<MachineFeatureSignal> getSignalClass() { return MachineFeatureSignal.class; }
}
