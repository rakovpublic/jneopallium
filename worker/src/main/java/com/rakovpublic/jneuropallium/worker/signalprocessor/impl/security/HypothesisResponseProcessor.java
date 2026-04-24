/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.EntityKind;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IResponsePlanningNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.QuarantineRequestSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: maps a {@link ThreatHypothesisSignal} through
 * the graduated-response planner. The first evidence id is used as the
 * target entity (callers that require richer routing can subclass).
 */
public class HypothesisResponseProcessor implements ISignalProcessor<ThreatHypothesisSignal, IResponsePlanningNeuron> {

    private static final String DESCRIPTION = "Graduated response planning";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ThreatHypothesisSignal input, IResponsePlanningNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        String entity = input.getEvidenceIds().isEmpty() ? input.getHypothesisId()
                : input.getEvidenceIds().get(0);
        QuarantineRequestSignal req = neuron.plan(input, entity, EntityKind.CONNECTION);
        if (req != null) out.add((I) req);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HypothesisResponseProcessor.class; }
    @Override public Class<IResponsePlanningNeuron> getNeuronClass() { return IResponsePlanningNeuron.class; }
    @Override public Class<ThreatHypothesisSignal> getSignalClass() { return ThreatHypothesisSignal.class; }
}
