/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;

/**
 * Layer 2 plant-mode state machine. STARTUP / NORMAL / SHUTDOWN /
 * EMERGENCY. Any interlock trip forces EMERGENCY; operator can request
 * transitions but an EMERGENCY state clears only by explicit reset.
 * Loop=2 / Epoch=1.
 */
public class ModeControllerNeuron extends ModulatableNeuron implements IModeControllerNeuron {

    private PlantMode mode = PlantMode.STARTUP;

    public ModeControllerNeuron() { super(); }
    public ModeControllerNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public PlantMode getMode() { return mode; }

    @Override
    public void requestMode(PlantMode requested) {
        if (requested == null || mode == PlantMode.EMERGENCY) return;
        this.mode = requested;
    }

    @Override
    public void onBatchState(BatchStateSignal s) {
        if (s == null) return;
        switch (s.getPhase()) {
            case RUNNING: if (mode != PlantMode.EMERGENCY) mode = PlantMode.NORMAL; break;
            case ABORTED:
            case STOPPED: mode = PlantMode.SHUTDOWN; break;
            default: break;
        }
    }

    @Override
    public void onInterlock(InterlockSignal s) {
        if (s != null && s.isTripped()) mode = PlantMode.EMERGENCY;
    }

    /** Operator-initiated reset from EMERGENCY back to SHUTDOWN. */
    public void resetFromEmergency() {
        if (mode == PlantMode.EMERGENCY) mode = PlantMode.SHUTDOWN;
    }

    @Override public boolean allowsNormalControl() { return mode == PlantMode.NORMAL || mode == PlantMode.STARTUP; }
}
