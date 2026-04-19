/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal;

/**
 * Layer 5 low-level actuator. Once the safety gate has approved a
 * {@link StimulationCommandSignal}, this neuron emits the physical-layer
 * command to the stim driver, and records the dispatch for transparency logs.
 * Loop=1 / Epoch=1.
 */
public class ActuatorNeuron extends ModulatableNeuron {

    private long dispatched;
    private long lastDispatchTick;

    public ActuatorNeuron() { super(); }
    public ActuatorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Record and dispatch a pre-approved stimulation command.
     * Returns true iff dispatched.
     */
    public boolean dispatch(StimulationCommandSignal cmd, long currentTick) {
        if (cmd == null) return false;
        dispatched++;
        lastDispatchTick = currentTick;
        return true;
    }

    public long getDispatchedCount() { return dispatched; }
    public long getLastDispatchTick() { return lastDispatchTick; }
    public void reset() { dispatched = 0; lastDispatchTick = 0; }
}
