/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Recommendation of the next content item, scored by zone-of-proximal-
 * development fit (Vygotsky 1978).
 * ProcessingFrequency: loop=1, epoch=3.
 */
public class ContentRecommendationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 1);

    private String itemId;
    private String rationale;
    private double expectedZPD;

    public ContentRecommendationSignal() {
        super();
        this.loop = 1;
        this.epoch = 3L;
        this.timeAlive = 100;
    }

    public ContentRecommendationSignal(String itemId, String rationale, double expectedZPD) {
        this();
        this.itemId = itemId;
        this.rationale = rationale;
        this.expectedZPD = Math.max(0.0, Math.min(1.0, expectedZPD));
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getRationale() { return rationale; }
    public void setRationale(String r) { this.rationale = r; }
    public double getExpectedZPD() { return expectedZPD; }
    public void setExpectedZPD(double v) { this.expectedZPD = Math.max(0.0, Math.min(1.0, v)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ContentRecommendationSignal.class; }
    @Override public String getDescription() { return "ContentRecommendationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ContentRecommendationSignal c = new ContentRecommendationSignal(itemId, rationale, expectedZPD);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
