/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

/**
 * Layer 2 user-state estimator. Fuses decoder stability, control-loop error,
 * and affect inputs into coarse labels: ALERT, FATIGUED, CONFUSED, DISTRESSED.
 * Used by the calibration scheduler and the stimulation safety gate to back
 * off when the user is out of nominal control.
 * Loop=1 / Epoch=3.
 */
public class UserStateNeuron extends ModulatableNeuron {

    public enum State { ALERT, FATIGUED, CONFUSED, DISTRESSED }

    private State state = State.ALERT;
    private double fatigueThreshold = 0.6;
    private double confusionThreshold = 0.5;
    private double distressThreshold = 0.7;

    public UserStateNeuron() { super(); }
    public UserStateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Classify the user state given rolling scores in [0,1].
     * {@code fatigue} is derived from decoder SNR / yawning / drift;
     * {@code confusion} from mismatch between intent and outcome;
     * {@code distress} from affect valence / arousal or pain reports.
     */
    public State classify(double fatigue, double confusion, double distress) {
        if (distress >= distressThreshold) state = State.DISTRESSED;
        else if (confusion >= confusionThreshold) state = State.CONFUSED;
        else if (fatigue >= fatigueThreshold) state = State.FATIGUED;
        else state = State.ALERT;
        return state;
    }

    public State getState() { return state; }
    public void setFatigueThreshold(double v) { this.fatigueThreshold = v; }
    public void setConfusionThreshold(double v) { this.confusionThreshold = v; }
    public void setDistressThreshold(double v) { this.distressThreshold = v; }
}
