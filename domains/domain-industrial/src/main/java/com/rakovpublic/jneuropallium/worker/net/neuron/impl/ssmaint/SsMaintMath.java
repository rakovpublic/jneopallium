/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

/** Small numeric helpers shared by the self-supervised maintenance neurons. */
public final class SsMaintMath {

    private SsMaintMath() { }

    public static double clamp01(double v) {
        if (!Double.isFinite(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    public static double clamp(double v, double lo, double hi) {
        if (!Double.isFinite(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }

    /** Ridge prediction: dot(weights[0..n-2], features) + bias(weights[n-1]). */
    public static double linear(double[] weights, double[] features) {
        if (weights == null || features == null || weights.length != features.length + 1) {
            return 0.0;
        }
        double acc = weights[weights.length - 1];
        for (int i = 0; i < features.length; i++) {
            acc += weights[i] * features[i];
        }
        return acc;
    }
}
