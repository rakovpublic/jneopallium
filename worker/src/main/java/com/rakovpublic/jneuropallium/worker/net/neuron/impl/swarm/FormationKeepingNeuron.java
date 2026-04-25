/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.FormationSignal;

/** Layer 4 formation-keeper. Loop=1 / Epoch=2. */
public class FormationKeepingNeuron extends ModulatableNeuron implements IFormationKeepingNeuron {

    private FormationSignal slot;

    public FormationKeepingNeuron() { super(); }
    public FormationKeepingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setSlot(FormationSignal s) { this.slot = s; }
    @Override public FormationSignal currentSlot() { return slot; }

    @Override
    public double[] steer(double[] currentRelativePosition) {
        if (slot == null || slot.getRelativeOffset() == null || currentRelativePosition == null) return new double[0];
        double[] target = slot.getRelativeOffset();
        int n = Math.min(target.length, currentRelativePosition.length);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = target[i] - currentRelativePosition[i];
        return out;
    }
}
