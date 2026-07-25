/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IReafferenceComparatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that stores an {@link EfferenceCopySignal} in an
 * {@link IReafferenceComparatorNeuron}'s pending-comparison buffer.
 * Mismatch detection happens when proprioceptive feedback later arrives.
 */
public class ReafferenceComparatorProcessor implements ISignalProcessor<EfferenceCopySignal, IReafferenceComparatorNeuron> {

    private static final String DESCRIPTION = "Captures efference copies for reafference comparison";

    @Override
    public <I extends ISignal> List<I> process(EfferenceCopySignal input, IReafferenceComparatorNeuron neuron) {
        if (input != null && neuron != null) neuron.registerEfferenceCopy(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ReafferenceComparatorProcessor.class; }
    @Override public Class<IReafferenceComparatorNeuron> getNeuronClass() { return IReafferenceComparatorNeuron.class; }
    @Override public Class<EfferenceCopySignal> getSignalClass() { return EfferenceCopySignal.class; }
}
