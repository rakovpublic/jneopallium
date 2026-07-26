/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 4 grip-type selector. Maps object geometry + intent into one of the
 * canonical grasp types (power, pinch, lateral, tripod). Biological analogue:
 * anterior intraparietal area (AIP) grip selection (Murata et al. 2000).
 * Loop=1 / Epoch=2.
 */
public class GripSelectionNeuron extends ModulatableNeuron implements IGripSelectionNeuron {

    public enum GripType { POWER, PINCH, LATERAL, TRIPOD, NONE }

    private GripType lastGrip = GripType.NONE;

    public GripSelectionNeuron() { super(); }
    public GripSelectionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Choose a grip from a rough object descriptor:
     * {@code sizeMeters} (longest axis) and {@code elongation} in [0,1].
     * Precision grasps for small / thin objects, power grasps for large ones.
     */
    public GripType select(double sizeMeters, double elongation, boolean thin) {
        if (sizeMeters <= 0) return lastGrip = GripType.NONE;
        if (thin && sizeMeters < 0.02) return lastGrip = GripType.PINCH;
        if (elongation > 0.7 && sizeMeters < 0.08) return lastGrip = GripType.LATERAL;
        if (sizeMeters < 0.05) return lastGrip = GripType.TRIPOD;
        return lastGrip = GripType.POWER;
    }

    public GripType getLastGrip() { return lastGrip; }
}
