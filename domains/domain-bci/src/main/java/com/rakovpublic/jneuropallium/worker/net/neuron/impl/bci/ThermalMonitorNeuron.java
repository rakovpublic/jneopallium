/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal;

/**
 * Layer 7 thermal monitor. Enforces ISO 14708 / IEEE C95 implant heating
 * limits: 1°C above baseline triggers a cool-down (duty-cycle reduction),
 * 2°C triggers an emergency shutdown of all stimulation.
 * Loop=2 / Epoch=1.
 */
public class ThermalMonitorNeuron extends ModulatableNeuron implements IThermalMonitorNeuron {

    private double coolDownDeltaC = 1.0;
    private double shutdownDeltaC = 2.0;
    private boolean coolDown;
    private boolean shutdown;
    private double lastDelta;

    public ThermalMonitorNeuron() { super(); }
    public ThermalMonitorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Ingest a thermal sample. Returns true if any state change was triggered.
     */
    public boolean observe(ThermalSignal s) {
        if (s == null) return false;
        lastDelta = s.getDeltaFromBaseline();
        boolean prevCD = coolDown;
        boolean prevSD = shutdown;
        if (lastDelta >= shutdownDeltaC) { shutdown = true; coolDown = true; }
        else if (lastDelta >= coolDownDeltaC) { coolDown = true; }
        else if (lastDelta < coolDownDeltaC * 0.5) { coolDown = false; shutdown = false; }
        return coolDown != prevCD || shutdown != prevSD;
    }

    public boolean isCoolDown() { return coolDown; }
    public boolean isShutdown() { return shutdown; }
    public double getLastDelta() { return lastDelta; }
    public void setCoolDownDeltaC(double d) { this.coolDownDeltaC = d; }
    public void setShutdownDeltaC(double d) { this.shutdownDeltaC = d; }
    public void reset() { coolDown = false; shutdown = false; lastDelta = 0; }
}
