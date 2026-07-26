/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IUserStateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.AgencyLossSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: folds an {@link AgencyLossSignal} into an
 * {@link IUserStateNeuron}'s distress axis so the user-state
 * classification reflects the mismatch magnitude.
 */
public class AgencyLossProcessor implements ISignalProcessor<AgencyLossSignal, IUserStateNeuron> {

    private static final String DESCRIPTION = "Maps agency-loss mismatch onto user-state distress";

    @Override
    public <I extends ISignal> List<I> process(AgencyLossSignal input, IUserStateNeuron neuron) {
        if (input != null && neuron != null) neuron.observeAgencyLoss(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AgencyLossProcessor.class; }
    @Override public Class<IUserStateNeuron> getNeuronClass() { return IUserStateNeuron.class; }
    @Override public Class<AgencyLossSignal> getSignalClass() { return AgencyLossSignal.class; }
}
