/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import java.io.Serializable;

/**
 * Immutable snapshot of an affective neuron's current state.
 * Biological analogue: instantaneous read-out of the valence/arousal axes
 * used by the circumplex model of affect (Russell 1980).
 */
public final class AffectState implements Serializable {

    private final double valence;
    private final double arousal;
    private final long asOfTick;

    public AffectState(double valence, double arousal, long asOfTick) {
        this.valence = Math.max(-1.0, Math.min(1.0, valence));
        this.arousal = Math.max(0.0, Math.min(1.0, arousal));
        this.asOfTick = asOfTick;
    }

    public double getValence() { return valence; }
    public double getArousal() { return arousal; }
    public long getAsOfTick() { return asOfTick; }

    @Override
    public String toString() {
        return "AffectState{valence=" + valence + ", arousal=" + arousal + ", asOfTick=" + asOfTick + '}';
    }
}
