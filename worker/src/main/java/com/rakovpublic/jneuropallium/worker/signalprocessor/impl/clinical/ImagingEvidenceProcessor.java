/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.FindingCategory;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IDifferentialDiagnosisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: imaging findings update the differential
 * diagnosis posterior. ABNORMAL/CRITICAL with high confidence yields a
 * positive likelihood ratio scaled by reported confidence; NORMAL acts
 * as mildly negative evidence. The ICD-10 hint is encoded in the
 * region code via the convention {@code "REGION|icd10"}.
 */
public class ImagingEvidenceProcessor implements ISignalProcessor<ImagingFindingSignal, IDifferentialDiagnosisNeuron> {

    private static final String DESCRIPTION = "Bayesian update from imaging finding";

    @Override
    public <I extends ISignal> List<I> process(ImagingFindingSignal input, IDifferentialDiagnosisNeuron neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        String region = input.getRegionCode();
        if (region == null || !region.contains("|")) return new LinkedList<>();
        String icd10 = region.substring(region.indexOf('|') + 1);
        if (icd10.isEmpty()) return new LinkedList<>();
        double lr;
        FindingCategory cat = input.getCategory();
        switch (cat) {
            case CRITICAL: lr = 1.0 + 4.0 * input.getConfidence(); break;
            case ABNORMAL: lr = 1.0 + 2.0 * input.getConfidence(); break;
            case INCIDENTAL: lr = 1.0; break;
            case NORMAL: lr = 1.0 - 0.5 * input.getConfidence(); break;
            default: lr = 1.0;
        }
        neuron.update(icd10, lr, "img:" + input.getModality() + ":" + region);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ImagingEvidenceProcessor.class; }
    @Override public Class<IDifferentialDiagnosisNeuron> getNeuronClass() { return IDifferentialDiagnosisNeuron.class; }
    @Override public Class<ImagingFindingSignal> getSignalClass() { return ImagingFindingSignal.class; }
}
