/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 5 harm gate — per-loop shadow / advisory / autonomous mode
 * control per spec §7. SHADOW downgrades {@code execute} to false;
 * ADVISORY passes through unchanged (physician / operator confirms
 * downstream); AUTONOMOUS allows free execution. Loop=1 / Epoch=1.
 */
public class SafetyGateNeuron extends ModulatableNeuron implements ISafetyGateNeuron {

    private final Map<String, SafetyMode> perTag = new HashMap<>();
    private SafetyMode defaultMode = SafetyMode.ADVISORY;

    public SafetyGateNeuron() { super(); }
    public SafetyGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setModeFor(String tag, SafetyMode mode) {
        if (tag != null && mode != null) perTag.put(tag, mode);
    }
    @Override public SafetyMode modeFor(String tag) { return perTag.getOrDefault(tag, defaultMode); }
    @Override public SafetyMode defaultMode() { return defaultMode; }
    @Override public void setDefaultMode(SafetyMode m) { if (m != null) this.defaultMode = m; }

    @Override
    public ActuatorCommandSignal gate(ActuatorCommandSignal cmd) {
        if (cmd == null) return null;
        SafetyMode m = modeFor(cmd.getTag());
        ActuatorCommandSignal out = new ActuatorCommandSignal(
                cmd.getTag(), cmd.getTargetValue(), cmd.getCurrentValue(),
                m == SafetyMode.AUTONOMOUS || (m == SafetyMode.ADVISORY && cmd.isExecute()));
        if (m == SafetyMode.SHADOW) out.setExecute(false);
        return out;
    }
}
