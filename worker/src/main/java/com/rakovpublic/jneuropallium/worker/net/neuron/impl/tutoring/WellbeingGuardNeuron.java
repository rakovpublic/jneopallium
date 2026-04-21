/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.InterventionSignal;

/**
 * Layer 7 pedagogical harm gate. Tracks sustained frustration or
 * disengagement and emits escalating {@link InterventionSignal}s:
 * ENCOURAGE → BREAK → REDIRECT → ESCALATE_TO_HUMAN.
 * Loop=1 / Epoch=1.
 */
public class WellbeingGuardNeuron extends ModulatableNeuron implements IWellbeingGuardNeuron {

    private int consecutiveFrustrationTicks;
    private int consecutiveBoredomTicks;
    private int maxFrustrationTicks = 400;
    private int maxBoredomTicks = 600;
    private int escalationStrikes;

    public WellbeingGuardNeuron() { super(); }
    public WellbeingGuardNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public InterventionSignal assess(FlowStateKind state) {
        InterventionSignal out = null;
        if (state == FlowStateKind.FRUSTRATION || state == FlowStateKind.OVERLOAD) {
            consecutiveFrustrationTicks++;
            consecutiveBoredomTicks = 0;
        } else if (state == FlowStateKind.BOREDOM) {
            consecutiveBoredomTicks++;
            consecutiveFrustrationTicks = 0;
        } else {
            consecutiveFrustrationTicks = 0;
            consecutiveBoredomTicks = 0;
        }

        if (consecutiveFrustrationTicks >= maxFrustrationTicks) {
            escalationStrikes++;
            consecutiveFrustrationTicks = 0;
            InterventionType t;
            if (escalationStrikes == 1) t = InterventionType.ENCOURAGE;
            else if (escalationStrikes == 2) t = InterventionType.BREAK;
            else if (escalationStrikes == 3) t = InterventionType.REDIRECT;
            else t = InterventionType.ESCALATE_TO_HUMAN;
            out = new InterventionSignal(t, "sustained-frustration");
            out.setSourceNeuronId(this.getId());
        } else if (consecutiveBoredomTicks >= maxBoredomTicks) {
            consecutiveBoredomTicks = 0;
            out = new InterventionSignal(InterventionType.REDIRECT, "sustained-boredom");
            out.setSourceNeuronId(this.getId());
        }
        return out;
    }

    public int getEscalationStrikes() { return escalationStrikes; }
    public int getConsecutiveFrustrationTicks() { return consecutiveFrustrationTicks; }
    public int getConsecutiveBoredomTicks() { return consecutiveBoredomTicks; }
    public void setMaxFrustrationTicks(int n) { this.maxFrustrationTicks = Math.max(1, n); }
    public void setMaxBoredomTicks(int n) { this.maxBoredomTicks = Math.max(1, n); }
}
