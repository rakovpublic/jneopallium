/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Spaced-repetition schedule entry emitted for a concept. Combines Ebbinghaus
 * forgetting curve and SM-2 interval updates.
 * ProcessingFrequency: loop=2, epoch=5.
 */
public class ReviewScheduleSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private String conceptId;
    private long nextReviewTick;
    private double targetRetention;

    public ReviewScheduleSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 100;
    }

    public ReviewScheduleSignal(String conceptId, long nextReviewTick, double targetRetention) {
        this();
        this.conceptId = conceptId;
        this.nextReviewTick = nextReviewTick;
        this.targetRetention = Math.max(0.0, Math.min(1.0, targetRetention));
    }

    public String getConceptId() { return conceptId; }
    public void setConceptId(String c) { this.conceptId = c; }
    public long getNextReviewTick() { return nextReviewTick; }
    public void setNextReviewTick(long t) { this.nextReviewTick = t; }
    public double getTargetRetention() { return targetRetention; }
    public void setTargetRetention(double r) { this.targetRetention = Math.max(0.0, Math.min(1.0, r)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ReviewScheduleSignal.class; }
    @Override public String getDescription() { return "ReviewScheduleSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ReviewScheduleSignal c = new ReviewScheduleSignal(conceptId, nextReviewTick, targetRetention);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
