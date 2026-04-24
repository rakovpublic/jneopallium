/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IThreatHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: performs a Bayesian posterior update on the
 * ranked hypothesis set using the incoming anomaly score. The entity
 * id in the score is used as the hypothesis id — adjust at the caller
 * if a different routing is required.
 */
public class AnomalyHypothesisProcessor implements ISignalProcessor<AnomalyScoreSignal, IThreatHypothesisNeuron> {

    private static final String DESCRIPTION = "Anomaly-driven hypothesis posterior update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AnomalyScoreSignal input, IThreatHypothesisNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ThreatHypothesisSignal t = neuron.updateFromAnomaly(input, input.getEntityId());
        if (t != null) out.add((I) t);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AnomalyHypothesisProcessor.class; }
    @Override public Class<IThreatHypothesisNeuron> getNeuronClass() { return IThreatHypothesisNeuron.class; }
    @Override public Class<AnomalyScoreSignal> getSignalClass() { return AnomalyScoreSignal.class; }
}
