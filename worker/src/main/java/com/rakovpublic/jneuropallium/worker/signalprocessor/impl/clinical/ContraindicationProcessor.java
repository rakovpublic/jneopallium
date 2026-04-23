/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IContraindicationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: every {@link TreatmentProposalSignal} is run
 * through the {@link IContraindicationNeuron}. Any non-null
 * {@link ClinicalVetoSignal} is forwarded immediately so that the
 * harm-discriminator audit path captures it.
 */
public class ContraindicationProcessor implements ISignalProcessor<TreatmentProposalSignal, IContraindicationNeuron> {

    private static final String DESCRIPTION = "Hard contraindication filter for proposed treatments";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(TreatmentProposalSignal input, IContraindicationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ClinicalVetoSignal veto = neuron.evaluate(input);
        if (veto != null) out.add((I) veto);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ContraindicationProcessor.class; }
    @Override public Class<IContraindicationNeuron> getNeuronClass() { return IContraindicationNeuron.class; }
    @Override public Class<TreatmentProposalSignal> getSignalClass() { return TreatmentProposalSignal.class; }
}
