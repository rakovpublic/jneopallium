/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

/** Converts fault hypotheses into advisory health-score outputs. */
public class MachineHealthCorrelationNeuron extends ModulatableNeuron implements IMachineHealthCorrelationNeuron {

    private String modelVersion = "1.0.0-machine-health";

    public MachineHealthCorrelationNeuron() { super(); }
    public MachineHealthCorrelationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public MachineHealthAdvisorySignal correlate(FaultHypothesisSignal hypothesis) {
        if (hypothesis == null) return null;
        double maxFault = hypothesis.getFaultProbabilities().values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(0.0);
        double health = 1.0 - MachineSignalMath.clamp01(
                0.65 * hypothesis.getAnomalyProbability() + 0.25 * maxFault + 0.10 * hypothesis.getUncertainty());
        String action;
        if (hypothesis.getDomainShiftScore() > 0.65 && hypothesis.getAnomalyProbability() < 0.85) {
            action = "COLLECT_SITE_BASELINE";
        } else if (hypothesis.getAnomalyProbability() > 0.85 || maxFault > 0.75) {
            action = "INSPECT_WITHIN_24_HOURS";
        } else if (hypothesis.getAnomalyProbability() > 0.60 || maxFault > 0.55) {
            action = "INSPECT_WITHIN_72_HOURS";
        } else if (hypothesis.getAnomalyProbability() > 0.35) {
            action = "MONITOR_CLOSELY";
        } else {
            action = "MONITOR";
        }
        return new MachineHealthAdvisorySignal(hypothesis.getAssetId(), modelVersion, "ADVISORY",
                health, hypothesis.getAnomalyProbability(), hypothesis.getFaultProbabilities(),
                hypothesis.getUnknownAnomalyProbability(), hypothesis.getDomainShiftScore(),
                hypothesis.getUncertainty(), action, hypothesis.getEvidence(), false,
                hypothesis.getTimestamp());
    }

    @Override public void setModelVersion(String modelVersion) {
        if (modelVersion != null && !modelVersion.isBlank()) this.modelVersion = modelVersion;
    }

    @Override public String getModelVersion() { return modelVersion; }
}
