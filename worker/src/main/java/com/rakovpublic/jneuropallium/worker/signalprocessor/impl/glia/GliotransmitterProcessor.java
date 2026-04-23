/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.IAstrocyteNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.GliotransmitterSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an arriving {@link GliotransmitterSignal} is
 * (re)released by the target {@link IAstrocyteNeuron} with its
 * concentration and transmitter type, so the neuron's local release
 * bookkeeping stays in sync. Re-emits the neuron's own release for
 * downstream consumers.
 */
public class GliotransmitterProcessor implements ISignalProcessor<GliotransmitterSignal, IAstrocyteNeuron> {

    private static final String DESCRIPTION = "Astrocyte gliotransmitter release bookkeeping";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(GliotransmitterSignal input, IAstrocyteNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        GliotransmitterSignal emitted = neuron.release(input.getTransmitter(), input.getConcentration());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return GliotransmitterProcessor.class; }
    @Override public Class<IAstrocyteNeuron> getNeuronClass() { return IAstrocyteNeuron.class; }
    @Override public Class<GliotransmitterSignal> getSignalClass() { return GliotransmitterSignal.class; }
}
