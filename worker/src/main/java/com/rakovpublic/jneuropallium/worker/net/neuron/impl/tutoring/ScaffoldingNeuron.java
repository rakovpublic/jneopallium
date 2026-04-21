/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ScaffoldingSignal;

/**
 * Layer 4 scaffolding emitter. Fires when {@link FlowStateNeuron} classifies
 * the learner as OVERLOAD; chooses a scaffold type appropriate to the
 * current context. Based on Wood, Bruner, Ross 1976.
 * Loop=1 / Epoch=2.
 */
public class ScaffoldingNeuron extends ModulatableNeuron implements IScaffoldingNeuron {

    public ScaffoldingNeuron() { super(); }
    public ScaffoldingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Decide on a scaffold given the current flow state. Returns null when no
     * scaffold is warranted (flow / neutral / boredom).
     */
    public ScaffoldingSignal scaffoldFor(FlowStateKind state, String conceptId) {
        if (state != FlowStateKind.OVERLOAD && state != FlowStateKind.FRUSTRATION) {
            return null;
        }
        ScaffoldType type;
        switch (state) {
            case OVERLOAD:
                type = ScaffoldType.OUTLINE;
                break;
            case FRUSTRATION:
                type = ScaffoldType.WORKED_STEPS;
                break;
            default:
                type = ScaffoldType.REMINDER;
        }
        ScaffoldingSignal s = new ScaffoldingSignal(type, conceptId);
        s.setSourceNeuronId(this.getId());
        return s;
    }
}
