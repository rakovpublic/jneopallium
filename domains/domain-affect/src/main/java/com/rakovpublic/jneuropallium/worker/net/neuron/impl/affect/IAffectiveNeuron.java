/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

/**
 * Contract for neurons that track a valence/arousal affective state
 * and can modulate their firing threshold as a function of arousal.
 * <p>Biological analogue: amygdala + anterior insula projection neurons
 * (see Damasio 1996, Craig 2009).
 */
public interface IAffectiveNeuron extends INeuron {

    /**
     * @return the current immutable affect snapshot.
     */
    AffectState currentState();

    /**
     * Adjust firing threshold as a function of arousal.
     * Higher arousal → lower threshold (easier firing); biological
     * locus-coeruleus norepinephrine effect.
     *
     * @param arousalFactor current arousal in [0,1]
     */
    void modulateThreshold(double arousalFactor);

    /**
     * React to an incoming cognitive appraisal.
     *
     * @param s appraisal signal
     */
    void onAppraisal(AppraisalSignal s);
}
