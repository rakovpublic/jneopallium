/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IDrugInteractionMemoryNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records every {@link MedicationAdminSignal} in
 * the active-regimen set held by an {@link IDrugInteractionMemoryNeuron}
 * so subsequent treatment proposals can be checked for DDI hazards.
 */
public class MedicationRegimenProcessor implements ISignalProcessor<MedicationAdminSignal, IDrugInteractionMemoryNeuron> {

    private static final String DESCRIPTION = "Active-medication regimen tracker for DDI lookups";

    @Override
    public <I extends ISignal> List<I> process(MedicationAdminSignal input, IDrugInteractionMemoryNeuron neuron) {
        if (input != null && neuron != null && input.getRxNormCode() != null) {
            neuron.addActive(input.getRxNormCode());
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MedicationRegimenProcessor.class; }
    @Override public Class<IDrugInteractionMemoryNeuron> getNeuronClass() { return IDrugInteractionMemoryNeuron.class; }
    @Override public Class<MedicationAdminSignal> getSignalClass() { return MedicationAdminSignal.class; }
}
