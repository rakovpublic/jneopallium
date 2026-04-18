/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AffectIntegrationNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.IAffectiveNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AffectStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that routes {@link AppraisalSignal}s into an
 * {@link IAffectiveNeuron}; when the target is an
 * {@link AffectIntegrationNeuron} the processor also emits a broadcast
 * {@link AffectStateSignal}.
 */
public class AffectIntegrationProcessor implements ISignalProcessor<AppraisalSignal, IAffectiveNeuron> {

    private static final String DESCRIPTION = "Integrates appraisal into affective state; broadcasts AffectStateSignal from integration neurons";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AppraisalSignal input, IAffectiveNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        neuron.onAppraisal(input);
        if (neuron instanceof AffectIntegrationNeuron) {
            AffectIntegrationNeuron in = (AffectIntegrationNeuron) neuron;
            AffectStateSignal broadcast = in.integrate(
                    input.getGoalDelta(),
                    input.getNovelty(),
                    input.getControllability(),
                    0.0,
                    0.0);
            out.add((I) broadcast);
        }
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AffectIntegrationProcessor.class; }
    @Override public Class<IAffectiveNeuron> getNeuronClass() { return IAffectiveNeuron.class; }
    @Override public Class<AppraisalSignal> getSignalClass() { return AppraisalSignal.class; }
}
