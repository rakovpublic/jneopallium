/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.ISsAdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal;

import java.util.LinkedList;
import java.util.List;

/** Gates a hypothesis into a read-only advisory using the live thresholds. */
public class AdvisoryGateProcessor
        implements ISignalProcessor<HealthHypothesisSignal, ISsAdvisoryGateNeuron> {

    private static final String DESCRIPTION = "Advisory gate (read-only)";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(HealthHypothesisSignal input, ISsAdvisoryGateNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        MaintenanceAdvisorySignal advisory = neuron.gate(input);
        if (advisory != null) out.add((I) advisory);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AdvisoryGateProcessor.class; }
    @Override public Class<ISsAdvisoryGateNeuron> getNeuronClass() { return ISsAdvisoryGateNeuron.class; }
    @Override public Class<HealthHypothesisSignal> getSignalClass() { return HealthHypothesisSignal.class; }
}
