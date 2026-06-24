/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IFaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;

import java.util.LinkedList;
import java.util.List;

/** Feeds domain-shift context into a fault hypothesis neuron. */
public class DomainShiftContextProcessor implements ISignalProcessor<DomainShiftSignal, IFaultHypothesisNeuron> {

    private static final String DESCRIPTION = "Fault-hypothesis domain-shift context update";

    @Override
    public <I extends ISignal> List<I> process(DomainShiftSignal input, IFaultHypothesisNeuron neuron) {
        if (input != null && neuron != null) neuron.observeDomainShift(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return DomainShiftContextProcessor.class; }
    @Override public Class<IFaultHypothesisNeuron> getNeuronClass() { return IFaultHypothesisNeuron.class; }
    @Override public Class<DomainShiftSignal> getSignalClass() { return DomainShiftSignal.class; }
}
