/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.ISignaturePatternNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: runs every incoming {@link PacketSignal} through
 * an {@link ISignaturePatternNeuron} and forwards any match. Innate
 * first-line detection.
 */
public class PacketSignatureProcessor implements ISignalProcessor<PacketSignal, ISignaturePatternNeuron> {

    private static final String DESCRIPTION = "Innate signature matching on packets";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(PacketSignal input, ISignaturePatternNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        SignatureMatchSignal m = neuron.match(input);
        if (m != null) out.add((I) m);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PacketSignatureProcessor.class; }
    @Override public Class<ISignaturePatternNeuron> getNeuronClass() { return ISignaturePatternNeuron.class; }
    @Override public Class<PacketSignal> getSignalClass() { return PacketSignal.class; }
}
