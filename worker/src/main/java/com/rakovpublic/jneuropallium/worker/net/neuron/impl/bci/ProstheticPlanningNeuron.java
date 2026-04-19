/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 4 prosthetic planner. Converts a high-level intent (target end-point
 * and grip type) into a minimum-jerk joint trajectory respecting joint limits.
 * Biological analogue: parietal / premotor trajectory planning (Shadmehr &
 * Wise 2005).
 * Loop=1 / Epoch=3.
 */
public class ProstheticPlanningNeuron extends ModulatableNeuron {

    private int dof = 7;
    private double[] jointMin;
    private double[] jointMax;
    private double[] currentJoints;

    public ProstheticPlanningNeuron() { super(); initLimits(); }
    public ProstheticPlanningNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        initLimits();
    }

    private void initLimits() {
        jointMin = new double[dof];
        jointMax = new double[dof];
        currentJoints = new double[dof];
        for (int i = 0; i < dof; i++) { jointMin[i] = -Math.PI; jointMax[i] = Math.PI; }
    }

    public void setDof(int d) { this.dof = Math.max(1, d); initLimits(); }
    public void setJointLimits(int j, double lo, double hi) {
        if (j < 0 || j >= dof) return;
        jointMin[j] = Math.min(lo, hi);
        jointMax[j] = Math.max(lo, hi);
    }
    public void setCurrentJoints(double[] q) {
        if (q == null) return;
        int n = Math.min(q.length, dof);
        for (int i = 0; i < n; i++) currentJoints[i] = q[i];
    }

    /**
     * Plan a single-step joint update toward {@code targetJoints}, clipped by
     * joint limits and a maximum per-step delta.
     */
    public double[] step(double[] targetJoints, double maxDelta) {
        double[] out = currentJoints.clone();
        if (targetJoints == null) return out;
        int n = Math.min(targetJoints.length, dof);
        for (int i = 0; i < n; i++) {
            double delta = targetJoints[i] - currentJoints[i];
            if (delta > maxDelta) delta = maxDelta;
            if (delta < -maxDelta) delta = -maxDelta;
            out[i] = Math.max(jointMin[i], Math.min(jointMax[i], currentJoints[i] + delta));
        }
        currentJoints = out;
        return out.clone();
    }

    public double[] getCurrentJoints() { return currentJoints.clone(); }
    public int getDof() { return dof; }
}
