/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 1 Georgopoulos-style population vector decoder (Georgopoulos et al.
 * 1986). Each unit has a preferred direction; the population vector is the
 * rate-weighted sum of preferred directions.
 * Loop=1 / Epoch=1.
 */
public class PopulationVectorNeuron extends ModulatableNeuron implements IPopulationVectorNeuron {

    private final Map<Integer, double[]> preferredDirections = new HashMap<>();

    public PopulationVectorNeuron() { super(); }
    public PopulationVectorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void tunePreferredDirection(int unitId, double[] direction) {
        preferredDirections.put(unitId, normalize(direction));
    }

    public int tunedUnitCount() { return preferredDirections.size(); }

    /**
     * Compute the population vector from a map of per-unit firing rates.
     * Output is a 3D vector (or matches the dim of the tuned directions).
     */
    public double[] decode(Map<Integer, Double> rates) {
        double[] acc = null;
        int d = 0;
        for (Map.Entry<Integer, Double> e : rates.entrySet()) {
            double[] pd = preferredDirections.get(e.getKey());
            if (pd == null) continue;
            if (acc == null) { d = pd.length; acc = new double[d]; }
            for (int i = 0; i < d; i++) acc[i] += e.getValue() * pd[i];
        }
        return acc == null ? new double[]{0, 0, 0} : acc;
    }

    private static double[] normalize(double[] v) {
        if (v == null) return new double[]{0, 0, 0};
        double n = 0;
        for (double x : v) n += x * x;
        n = Math.sqrt(n);
        if (n == 0) return v.clone();
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i] / n;
        return out;
    }
}
