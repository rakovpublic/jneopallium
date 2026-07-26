/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.swarm;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm.IStigmergicMemoryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.StigmergicTraceSignal;

import java.util.LinkedList;
import java.util.List;

/** Stateless processor: deposits a hardware-deposited trace into the local stigmergic cache. */
public class StigmergicTraceDepositProcessor implements ISignalProcessor<StigmergicTraceSignal, IStigmergicMemoryNeuron> {

    private static final String DESCRIPTION = "Stigmergic trace deposit into memory";

    @Override
    public <I extends ISignal> List<I> process(StigmergicTraceSignal input, IStigmergicMemoryNeuron neuron) {
        if (input != null && neuron != null) neuron.deposit(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return StigmergicTraceDepositProcessor.class; }
    @Override public Class<IStigmergicMemoryNeuron> getNeuronClass() { return IStigmergicMemoryNeuron.class; }
    @Override public Class<StigmergicTraceSignal> getSignalClass() { return StigmergicTraceSignal.class; }
}
