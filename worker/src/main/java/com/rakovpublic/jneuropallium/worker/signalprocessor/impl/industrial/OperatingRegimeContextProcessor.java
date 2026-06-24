/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IFaultHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

import java.util.LinkedList;
import java.util.List;

/** Feeds operating-regime context into a fault hypothesis neuron. */
public class OperatingRegimeContextProcessor implements ISignalProcessor<OperatingRegimeSignal, IFaultHypothesisNeuron> {

    private static final String DESCRIPTION = "Fault-hypothesis operating-regime context update";

    @Override
    public <I extends ISignal> List<I> process(OperatingRegimeSignal input, IFaultHypothesisNeuron neuron) {
        if (input != null && neuron != null) neuron.observeRegime(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return OperatingRegimeContextProcessor.class; }
    @Override public Class<IFaultHypothesisNeuron> getNeuronClass() { return IFaultHypothesisNeuron.class; }
    @Override public Class<OperatingRegimeSignal> getSignalClass() { return OperatingRegimeSignal.class; }
}
