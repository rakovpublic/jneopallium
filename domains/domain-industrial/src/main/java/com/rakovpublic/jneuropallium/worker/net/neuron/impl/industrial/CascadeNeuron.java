/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

/**
 * Layer 1 cascade router. Passes the outer-PID output as a fresh
 * setpoint to the inner loop. When {@link #setBroken(boolean) broken},
 * the cascade emits the last-good setpoint — implementation of the
 * spec §6 {@link OscillationIntervention#BREAK_CONNECTION} intervention.
 * Loop=1 / Epoch=1.
 */
public class CascadeNeuron extends ModulatableNeuron implements ICascadeNeuron {

    private SetpointSignal lastInner;
    private boolean broken;

    public CascadeNeuron() { super(); }
    public CascadeNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public SetpointSignal forward(ActuatorCommandSignal outerOutput, String innerTag) {
        if (broken) return lastInner;
        if (outerOutput == null) return null;
        SetpointSignal sp = new SetpointSignal(innerTag, outerOutput.getTargetValue(), 0.0, "cascade");
        lastInner = sp;
        return sp;
    }

    @Override public void setBroken(boolean broken) { this.broken = broken; }
    @Override public boolean isBroken() { return broken; }
    @Override public SetpointSignal lastInnerSetpoint() { return lastInner; }
}
