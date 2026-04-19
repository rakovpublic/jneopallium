/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Carries a learner response to a presented item.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class ResponseSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String itemId;
    private boolean correct;
    private long latencyMs;
    private String responsePayload;

    public ResponseSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public ResponseSignal(String itemId, boolean correct, long latencyMs, String responsePayload) {
        this();
        this.itemId = itemId;
        this.correct = correct;
        this.latencyMs = latencyMs;
        this.responsePayload = responsePayload;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getResponsePayload() { return responsePayload; }
    public void setResponsePayload(String p) { this.responsePayload = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ResponseSignal.class; }
    @Override public String getDescription() { return "ResponseSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ResponseSignal c = new ResponseSignal(itemId, correct, latencyMs, responsePayload);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
