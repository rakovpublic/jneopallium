/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

import java.util.List;

/**
 * Layer 4 Reynolds-rules flocking. Returns a steering vector (in the
 * same dimensionality as neighbours' positions) blending separation,
 * alignment, cohesion. Each rule is weighted by neighbour link
 * quality so unreliable neighbours don't dominate. Loop=1 / Epoch=1.
 */
public class FlockingNeuron extends ModulatableNeuron implements IFlockingNeuron {

    private double separation = 1.0;
    private double alignment = 0.7;
    private double cohesion = 0.5;
    private double radius = 5.0;

    public FlockingNeuron() { super(); }
    public FlockingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setWeights(double sep, double align, double coh) {
        this.separation = sep;
        this.alignment = align;
        this.cohesion = coh;
    }
    @Override public double getSeparationWeight() { return separation; }
    @Override public double getAlignmentWeight() { return alignment; }
    @Override public double getCohesionWeight() { return cohesion; }
    @Override public void setRadius(double r) { this.radius = Math.max(0.0, r); }
    @Override public double getRadius() { return radius; }

    @Override
    public double[] steer(List<PeerObservationSignal> neighbours) {
        if (neighbours == null || neighbours.isEmpty()) return new double[0];
        int dim = -1;
        for (PeerObservationSignal n : neighbours) {
            if (n != null && n.getPositionLocal() != null) { dim = n.getPositionLocal().length; break; }
        }
        if (dim <= 0) return new double[0];

        double[] sep = new double[dim], coh = new double[dim], ali = new double[dim];
        double weightSum = 0.0;
        int n = 0;
        for (PeerObservationSignal p : neighbours) {
            if (p == null || p.getPositionLocal() == null) continue;
            double[] pos = p.getPositionLocal();
            double dist = 0.0;
            for (int i = 0; i < Math.min(dim, pos.length); i++) dist += pos[i] * pos[i];
            dist = Math.sqrt(dist);
            if (dist > radius) continue;
            double w = p.getLinkQuality();
            weightSum += w;
            n++;
            for (int i = 0; i < Math.min(dim, pos.length); i++) {
                if (dist > 1e-6) sep[i] += w * (-pos[i] / Math.max(0.5, dist));
                coh[i] += w * pos[i];
                if (p.getVelocityLocal() != null && i < p.getVelocityLocal().length) {
                    ali[i] += w * p.getVelocityLocal()[i];
                }
            }
        }
        if (weightSum <= 0 || n == 0) return new double[dim];
        double[] out = new double[dim];
        for (int i = 0; i < dim; i++) {
            out[i] = separation * (sep[i] / weightSum)
                   + cohesion   * (coh[i] / weightSum)
                   + alignment  * (ali[i] / weightSum);
        }
        return out;
    }
}
