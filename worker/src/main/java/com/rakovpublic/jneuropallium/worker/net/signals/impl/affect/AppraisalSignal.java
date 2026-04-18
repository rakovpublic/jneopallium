/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Cognitive appraisal of a situation: goal progress, novelty, controllability.
 * Biological analogue: medial prefrontal cortex + orbitofrontal cortex
 * appraisal outputs feeding the limbic integrator.
 * <p>ProcessingFrequency: loop=1 (fast), epoch=2.
 */
public class AppraisalSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private double goalDelta;
    private double novelty;
    private double controllability;

    public AppraisalSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 100;
    }

    public AppraisalSignal(double goalDelta, double novelty, double controllability) {
        this();
        this.goalDelta = goalDelta;
        this.novelty = novelty;
        this.controllability = controllability;
    }

    public double getGoalDelta() { return goalDelta; }
    public void setGoalDelta(double goalDelta) { this.goalDelta = goalDelta; }

    public double getNovelty() { return novelty; }
    public void setNovelty(double novelty) { this.novelty = novelty; }

    public double getControllability() { return controllability; }
    public void setControllability(double controllability) { this.controllability = controllability; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AppraisalSignal.class; }
    @Override public String getDescription() { return "AppraisalSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AppraisalSignal c = new AppraisalSignal(goalDelta, novelty, controllability);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
