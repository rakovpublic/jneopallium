/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IChargeBalanceNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ChargeAccumulationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: cross-checks a {@link ChargeAccumulationSignal}
 * against an {@link IChargeBalanceNeuron}'s local bookkeeping. When the
 * electrode's DC drift exceeds tolerance the signal is re-emitted as an
 * audit breadcrumb.
 */
public class ChargeAccumulationProcessor implements ISignalProcessor<ChargeAccumulationSignal, IChargeBalanceNeuron> {

    private static final String DESCRIPTION = "Charge-balance cross-check";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ChargeAccumulationSignal input, IChargeBalanceNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        if (neuron.observe(input)) out.add((I) input);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ChargeAccumulationProcessor.class; }
    @Override public Class<IChargeBalanceNeuron> getNeuronClass() { return IChargeBalanceNeuron.class; }
    @Override public Class<ChargeAccumulationSignal> getSignalClass() { return ChargeAccumulationSignal.class; }
}
