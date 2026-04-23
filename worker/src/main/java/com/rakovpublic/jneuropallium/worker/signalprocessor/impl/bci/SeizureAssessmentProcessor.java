/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ISeizureWatchdogNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: runs an {@link LFPSignal} through an
 * {@link ISeizureWatchdogNeuron} and forwards any resulting
 * {@link SeizureRiskSignal}. LFP channel id is used as the "region"
 * argument (a real deployment can override with a dedicated mapping).
 */
public class SeizureAssessmentProcessor implements ISignalProcessor<LFPSignal, ISeizureWatchdogNeuron> {

    private static final String DESCRIPTION = "LFP → seizure risk assessment";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(LFPSignal input, ISeizureWatchdogNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        SeizureRiskSignal risk = neuron.assess(input, input.getChannelId());
        if (risk != null) out.add((I) risk);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SeizureAssessmentProcessor.class; }
    @Override public Class<ISeizureWatchdogNeuron> getNeuronClass() { return ISeizureWatchdogNeuron.class; }
    @Override public Class<LFPSignal> getSignalClass() { return LFPSignal.class; }
}
