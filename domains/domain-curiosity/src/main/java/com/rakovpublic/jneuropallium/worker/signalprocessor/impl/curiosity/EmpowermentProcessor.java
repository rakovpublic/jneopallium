/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity.IEmpowermentNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.EmpowermentSignal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: triggers an empowerment re-estimation for the
 * state identified by an incoming {@link EmpowermentSignal}. With no
 * rollouts attached (the signal is a request, not a forecast), the
 * neuron returns its cached estimate for that state and the processor
 * forwards it downstream as a re-emitted {@link EmpowermentSignal}.
 */
public class EmpowermentProcessor implements ISignalProcessor<EmpowermentSignal, IEmpowermentNeuron> {

    private static final String DESCRIPTION = "Mutual-information empowerment estimation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(EmpowermentSignal input, IEmpowermentNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        EmpowermentSignal emitted = neuron.estimate(input.getStateId(), Collections.emptyList());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return EmpowermentProcessor.class; }
    @Override public Class<IEmpowermentNeuron> getNeuronClass() { return IEmpowermentNeuron.class; }
    @Override public Class<EmpowermentSignal> getSignalClass() { return EmpowermentSignal.class; }
}
