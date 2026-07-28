/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IDifferentialDiagnosisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: an abnormal {@link LabResultSignal} is treated
 * as evidence of moderate strength (likelihood ratio 2.0) for any
 * candidate already seeded in the {@link IDifferentialDiagnosisNeuron}
 * whose ICD-10 code maps to the analyte. Mapping itself is the
 * deployment's responsibility — this processor only forwards the LR
 * for an explicit ICD-10 hint carried in the lab signal's analyte
 * code via the convention {@code "LOINC:icd10"}; otherwise no update.
 */
public class LabEvidenceProcessor implements ISignalProcessor<LabResultSignal, IDifferentialDiagnosisNeuron> {

    private static final String DESCRIPTION = "Bayesian update from abnormal lab result";

    private static final double ABNORMAL_LR = 2.0;
    private static final double NORMAL_LR = 0.5;

    @Override
    public <I extends ISignal> List<I> process(LabResultSignal input, IDifferentialDiagnosisNeuron neuron) {
        if (input == null || neuron == null) return new LinkedList<>();
        String code = input.getAnalyteCode();
        if (code == null || !code.contains(":")) return new LinkedList<>();
        String icd10 = code.substring(code.indexOf(':') + 1);
        if (icd10.isEmpty()) return new LinkedList<>();
        double lr = input.isAbnormal() ? ABNORMAL_LR : NORMAL_LR;
        neuron.update(icd10, lr, "lab:" + input.getAnalyteCode());
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return LabEvidenceProcessor.class; }
    @Override public Class<IDifferentialDiagnosisNeuron> getNeuronClass() { return IDifferentialDiagnosisNeuron.class; }
    @Override public Class<LabResultSignal> getSignalClass() { return LabResultSignal.class; }
}
