/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia.IMyelinationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.MyelinationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records usage of a source → target axon in an
 * {@link IMyelinationNeuron}; if the activity window has filled, the
 * neuron emits a follow-up {@link MyelinationSignal} with the reduced
 * delay, which this processor forwards. Myelination only reduces delay
 * (never raises it), matching the activity-dependent biology.
 */
public class MyelinationProcessor implements ISignalProcessor<MyelinationSignal, IMyelinationNeuron> {

    private static final String DESCRIPTION = "Activity-dependent axon myelination";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MyelinationSignal input, IMyelinationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.recordUsage(input.getAxonTargetId());
        MyelinationSignal emitted = neuron.evaluate(input.getAxonSourceId(), input.getAxonTargetId());
        if (emitted != null) out.add((I) emitted);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MyelinationProcessor.class; }
    @Override public Class<IMyelinationNeuron> getNeuronClass() { return IMyelinationNeuron.class; }
    @Override public Class<MyelinationSignal> getSignalClass() { return MyelinationSignal.class; }
}
