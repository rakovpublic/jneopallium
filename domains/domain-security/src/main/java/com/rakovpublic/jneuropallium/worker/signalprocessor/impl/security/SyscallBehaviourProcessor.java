/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IProcessBehaviourNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: hands each {@link SyscallSignal} to an
 * {@link IProcessBehaviourNeuron} and forwards any matched sequence
 * as a {@link SignatureMatchSignal}.
 */
public class SyscallBehaviourProcessor implements ISignalProcessor<SyscallSignal, IProcessBehaviourNeuron> {

    private static final String DESCRIPTION = "Forbidden-sequence syscall matcher";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SyscallSignal input, IProcessBehaviourNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        SignatureMatchSignal m = neuron.observe(input);
        if (m != null) out.add((I) m);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SyscallBehaviourProcessor.class; }
    @Override public Class<IProcessBehaviourNeuron> getNeuronClass() { return IProcessBehaviourNeuron.class; }
    @Override public Class<SyscallSignal> getSignalClass() { return SyscallSignal.class; }
}
