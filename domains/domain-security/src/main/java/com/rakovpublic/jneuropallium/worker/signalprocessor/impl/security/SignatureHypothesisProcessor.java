/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.IThreatHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: updates hypothesis posteriors from a
 * {@link SignatureMatchSignal}. The signature's family is used as the
 * hypothesis id when present; callers can override by seeding aliases.
 */
public class SignatureHypothesisProcessor implements ISignalProcessor<SignatureMatchSignal, IThreatHypothesisNeuron> {

    private static final String DESCRIPTION = "Signature-driven hypothesis posterior update";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(SignatureMatchSignal input, IThreatHypothesisNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        String id = input.getFamily() != null ? input.getFamily() : input.getSignatureId();
        ThreatHypothesisSignal t = neuron.updateFromSignature(input, id);
        if (t != null) out.add((I) t);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SignatureHypothesisProcessor.class; }
    @Override public Class<IThreatHypothesisNeuron> getNeuronClass() { return IThreatHypothesisNeuron.class; }
    @Override public Class<SignatureMatchSignal> getSignalClass() { return SignatureMatchSignal.class; }
}
