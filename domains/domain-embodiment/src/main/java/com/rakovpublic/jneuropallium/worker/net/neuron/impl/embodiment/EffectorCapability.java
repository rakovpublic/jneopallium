/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Immutable description of an effector's capabilities.
 * Biological analogue: posterior parietal body schema's entry for a single
 * limb/tool (Maravita &amp; Iriki 2004).
 */
public final class EffectorCapability implements Serializable {

    private final int dof;
    private final double[] rangeMin;
    private final double[] rangeMax;
    private final double health;
    private final boolean damaged;

    public EffectorCapability(int dof, double[] rangeMin, double[] rangeMax, double health, boolean damaged) {
        this.dof = dof;
        this.rangeMin = rangeMin == null ? new double[0] : rangeMin.clone();
        this.rangeMax = rangeMax == null ? new double[0] : rangeMax.clone();
        this.health = Math.max(0.0, Math.min(1.0, health));
        this.damaged = damaged;
    }

    public int getDof() { return dof; }
    public double[] getRangeMin() { return rangeMin.clone(); }
    public double[] getRangeMax() { return rangeMax.clone(); }
    public double getHealth() { return health; }
    public boolean isDamaged() { return damaged; }

    @Override public String toString() {
        return "EffectorCapability{dof=" + dof + ", health=" + health
                + ", damaged=" + damaged
                + ", min=" + Arrays.toString(rangeMin)
                + ", max=" + Arrays.toString(rangeMax) + '}';
    }
}
