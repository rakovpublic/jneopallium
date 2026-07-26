/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 5 stimulation safety gate. Vetoes any {@link StimulationCommandSignal}
 * that violates a hard safety envelope: Shannon charge-density criterion
 * (Shannon 1992; Cogan 2008), per-phase charge limit, maximum frequency /
 * pulse width, seizure lockout, and thermal lockout. Non-negotiable — must
 * sit between {@code ProstheticPlanningNeuron} and any stim actuator.
 * Loop=1 / Epoch=1.
 */
public class StimulationSafetyGateNeuron extends ModulatableNeuron implements IStimulationSafetyGateNeuron {

    /** Shannon criterion default (µC/cm² per phase). */
    private double maxChargeDensityUCcm2 = 0.5;
    private double maxChargePerPhaseUC = 2.0;
    private double maxFreqHz = 300.0;
    private double maxPulseWidthUS = 300.0;
    private double minElectrodeAreaCm2 = 1e-5;

    private final Map<Integer, Double> electrodeAreaCm2 = new HashMap<>();

    private boolean seizureLockout;
    private boolean thermalLockout;
    private long lockoutUntilTick;

    public StimulationSafetyGateNeuron() { super(); }
    public StimulationSafetyGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setElectrodeArea(int electrodeId, double cm2) {
        electrodeAreaCm2.put(electrodeId, Math.max(minElectrodeAreaCm2, cm2));
    }

    public void setMaxChargeDensityUCcm2(double v) { this.maxChargeDensityUCcm2 = Math.max(0, v); }
    public void setMaxChargePerPhaseUC(double v) { this.maxChargePerPhaseUC = Math.max(0, v); }
    public void setMaxFrequencyHz(double v) { this.maxFreqHz = Math.max(0, v); }
    public void setMaxPulseWidthUS(double v) { this.maxPulseWidthUS = Math.max(0, v); }

    public void triggerSeizureLockout(long untilTick) { this.seizureLockout = true; this.lockoutUntilTick = untilTick; }
    public void triggerThermalLockout(long untilTick) { this.thermalLockout = true; this.lockoutUntilTick = untilTick; }
    public void clearLockouts(long currentTick) {
        if (currentTick >= lockoutUntilTick) { seizureLockout = false; thermalLockout = false; }
    }

    public boolean isLocked() { return seizureLockout || thermalLockout; }

    /**
     * Evaluate a stimulation command. Returns null (allow) if safe; otherwise
     * a human-readable veto reason. Callers must treat non-null as a hard
     * refusal and emit a TransparencyLogSignal upstream.
     */
    public String veto(StimulationCommandSignal cmd, long currentTick) {
        if (cmd == null) return "null_command";
        clearLockouts(currentTick);
        if (seizureLockout) return "seizure_lockout";
        if (thermalLockout) return "thermal_lockout";
        if (cmd.getFrequencyHz() > maxFreqHz) return "frequency_exceeds_max";
        if (cmd.getPulseWidthUS() > maxPulseWidthUS) return "pulse_width_exceeds_max";
        double qPhase = cmd.chargePerPhaseUC();
        if (qPhase > maxChargePerPhaseUC) return "charge_per_phase_exceeds_max";
        double areaCm2 = electrodeAreaCm2.getOrDefault(cmd.getElectrodeId(), minElectrodeAreaCm2);
        double density = qPhase / areaCm2;
        if (density > maxChargeDensityUCcm2) return "charge_density_exceeds_shannon";
        if (cmd.getNPulses() < 0) return "negative_pulse_count";
        return null;
    }
}
