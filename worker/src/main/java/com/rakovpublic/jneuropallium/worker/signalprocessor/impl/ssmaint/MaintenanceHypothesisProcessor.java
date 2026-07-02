/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.IMaintenanceHypothesisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;

import java.util.LinkedList;
import java.util.List;

/** Fuses reconstruction residuals into a label-free maintenance hypothesis. */
public class MaintenanceHypothesisProcessor
        implements ISignalProcessor<ReconResidualSignal, IMaintenanceHypothesisNeuron> {

    private static final String DESCRIPTION = "Maintenance hypothesis fusion";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ReconResidualSignal input, IMaintenanceHypothesisNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        HealthHypothesisSignal h = neuron.assess(input);
        if (h != null) out.add((I) h);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MaintenanceHypothesisProcessor.class; }
    @Override public Class<IMaintenanceHypothesisNeuron> getNeuronClass() { return IMaintenanceHypothesisNeuron.class; }
    @Override public Class<ReconResidualSignal> getSignalClass() { return ReconResidualSignal.class; }
}
