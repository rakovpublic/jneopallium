/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ChargeAccumulationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 5 charge balance monitor. Tracks running net DC charge per electrode;
 * persistent DC offset accelerates electrode / tissue damage. Emits a
 * {@link ChargeAccumulationSignal} and vetoes further stimulation on channels
 * that exceed the configured DC tolerance (default ± 1 nC).
 * Loop=1 / Epoch=1.
 */
public class ChargeBalanceNeuron extends ModulatableNeuron {

    private final Map<Integer, Double> netChargeUC = new HashMap<>();
    private double dcToleranceUC = 1e-3;  // 1 nC = 1e-3 µC

    public ChargeBalanceNeuron() { super(); }
    public ChargeBalanceNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Accumulate charge from a biphasic command; asymmetric pulses are
     * recorded as residual DC (positive = anodic bias, negative = cathodic).
     */
    public ChargeAccumulationSignal accumulate(StimulationCommandSignal cmd, double anodicImbalanceFrac) {
        if (cmd == null) return null;
        double qPhase = cmd.chargePerPhaseUC();
        double residual = qPhase * anodicImbalanceFrac * cmd.getNPulses();
        double prev = netChargeUC.getOrDefault(cmd.getElectrodeId(), 0.0);
        double updated = prev + residual;
        netChargeUC.put(cmd.getElectrodeId(), updated);
        double densityUCm2 = updated;
        return new ChargeAccumulationSignal(cmd.getElectrodeId(), updated, densityUCm2);
    }

    public boolean exceedsDc(int electrodeId) {
        return Math.abs(netChargeUC.getOrDefault(electrodeId, 0.0)) > dcToleranceUC;
    }

    public double getNetCharge(int electrodeId) { return netChargeUC.getOrDefault(electrodeId, 0.0); }
    public void resetElectrode(int electrodeId) { netChargeUC.remove(electrodeId); }
    public void setDcToleranceUC(double v) { this.dcToleranceUC = Math.max(0, v); }
}
