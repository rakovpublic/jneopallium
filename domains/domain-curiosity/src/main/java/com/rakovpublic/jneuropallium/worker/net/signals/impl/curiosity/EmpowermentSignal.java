/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Estimate of mutual information between action choice and future state
 * (empowerment, Klyubin et al. 2005) — an agent-centric measure of
 * how much control the agent retains from a given state.
 * <p>ProcessingFrequency: loop=2, epoch=3.
 */
public class EmpowermentSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private int stateId;
    private double mutualInformation;

    public EmpowermentSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 100;
    }

    public EmpowermentSignal(int stateId, double mutualInformation) {
        this();
        this.stateId = stateId;
        this.mutualInformation = Math.max(0.0, mutualInformation);
    }

    public int getStateId() { return stateId; }
    public void setStateId(int stateId) { this.stateId = stateId; }

    public double getMutualInformation() { return mutualInformation; }
    public void setMutualInformation(double mutualInformation) { this.mutualInformation = Math.max(0.0, mutualInformation); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EmpowermentSignal.class; }
    @Override public String getDescription() { return "EmpowermentSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EmpowermentSignal c = new EmpowermentSignal(stateId, mutualInformation);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
