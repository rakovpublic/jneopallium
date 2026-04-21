/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 1 Kalman state-space decoder for cursor / end-effector kinematics
 * (Wu et al. 2006). Held as a scalar 2-state (position, velocity) filter
 * here; production deployments swap in a full KF with matrix dynamics.
 * Loop=1 / Epoch=1.
 */
public class KalmanDecoderNeuron extends ModulatableNeuron implements IKalmanDecoderNeuron {

    private double pos;
    private double vel;
    private double varP = 1.0;   // position uncertainty
    private double varV = 1.0;   // velocity uncertainty
    private double processNoise = 0.01;
    private double measurementNoise = 0.1;
    private double c = 1.0;      // observation coefficient

    public KalmanDecoderNeuron() { super(); }
    public KalmanDecoderNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Predict then update given a scalar neural observation {@code y}.
     * Returns the new position estimate.
     */
    public double step(double y) {
        pos = pos + vel;
        varP = varP + varV + processNoise;
        double k = (c * varP) / (c * c * varP + measurementNoise);
        double innovation = y - c * pos;
        pos = pos + k * innovation;
        vel = vel + 0.5 * k * innovation;
        varP = (1 - k * c) * varP;
        return pos;
    }

    public double getPos() { return pos; }
    public double getVel() { return vel; }
    public void setProcessNoise(double p) { this.processNoise = Math.max(0, p); }
    public void setMeasurementNoise(double m) { this.measurementNoise = Math.max(1e-9, m); }
    public void setObservationCoefficient(double c) { this.c = c; }
    public void reset() { pos = 0; vel = 0; varP = 1.0; varV = 1.0; }
}
