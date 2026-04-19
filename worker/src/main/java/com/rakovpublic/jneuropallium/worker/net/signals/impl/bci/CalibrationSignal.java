/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.CalibrationTarget;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Calibration-session event or summary. Emitted at session boundaries to
 * record what was tuned and how well it performed.
 * ProcessingFrequency: loop=2, epoch=3.
 */
public class CalibrationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String sessionId;
    private CalibrationTarget target;
    private double performanceScore;

    public CalibrationSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 500;
        this.target = CalibrationTarget.DECODER_WEIGHTS;
    }

    public CalibrationSignal(String sessionId, CalibrationTarget target, double performanceScore) {
        this();
        this.sessionId = sessionId;
        this.target = target == null ? CalibrationTarget.DECODER_WEIGHTS : target;
        this.performanceScore = Math.max(0.0, Math.min(1.0, performanceScore));
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String s) { this.sessionId = s; }
    public CalibrationTarget getTarget() { return target; }
    public void setTarget(CalibrationTarget t) { this.target = t == null ? CalibrationTarget.DECODER_WEIGHTS : t; }
    public double getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(double v) { this.performanceScore = Math.max(0.0, Math.min(1.0, v)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return CalibrationSignal.class; }
    @Override public String getDescription() { return "CalibrationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        CalibrationSignal c = new CalibrationSignal(sessionId, target, performanceScore);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
