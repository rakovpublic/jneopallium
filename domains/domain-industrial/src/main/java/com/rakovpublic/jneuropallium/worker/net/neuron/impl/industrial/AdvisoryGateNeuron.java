/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

import java.util.ArrayList;
import java.util.List;

/** Final advisory gate: learned health intelligence never becomes direct control. */
public class AdvisoryGateNeuron extends ModulatableNeuron implements IAdvisoryGateNeuron {

    private double domainShiftHoldThreshold = 0.65;
    private double uncertaintyHoldThreshold = 0.55;

    public AdvisoryGateNeuron() { super(); }
    public AdvisoryGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public double getDomainShiftHoldThreshold() {
        return domainShiftHoldThreshold;
    }

    public double getUncertaintyHoldThreshold() {
        return uncertaintyHoldThreshold;
    }

    @Override
    public MachineHealthAdvisorySignal gate(MachineHealthAdvisorySignal advisory) {
        if (advisory == null) return null;
        List<String> evidence = new ArrayList<>(advisory.getEvidence());
        String action = advisory.getRecommendedAction();
        if (advisory.getDomainShiftScore() >= domainShiftHoldThreshold
                && advisory.getAnomalyProbability() < 0.85) {
            action = "COLLECT_SITE_BASELINE";
            evidence.add("domain-shift gate holds inspection escalation until baseline is adapted");
        }
        if (advisory.getUncertainty() >= uncertaintyHoldThreshold
                && advisory.getAnomalyProbability() < 0.85) {
            evidence.add("uncertainty gate requires human review before maintenance escalation");
        }
        MachineHealthAdvisorySignal gated = new MachineHealthAdvisorySignal(advisory.getAssetId(), advisory.getModelVersion(),
                "ADVISORY", advisory.getHealthScore(), advisory.getAnomalyProbability(),
                advisory.getFaultProbabilities(), advisory.getUnknownAnomalyProbability(),
                advisory.getDomainShiftScore(), advisory.getUncertainty(), action,
                evidence, false, advisory.getTimestamp());
        gated.sourceLayer = advisory.sourceLayer;
        gated.sourceNeuron = advisory.sourceNeuron;
        return gated;
    }

    @Override public void setDomainShiftHoldThreshold(double threshold) {
        this.domainShiftHoldThreshold = MachineSignalMath.clamp01(threshold);
    }

    @Override public void setUncertaintyHoldThreshold(double threshold) {
        this.uncertaintyHoldThreshold = MachineSignalMath.clamp01(threshold);
    }
}
