/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Emitted by the feedback-adaptation neuron whenever it moves a fault family's
 * decision threshold. The advisory gate consumes it to update its live
 * thresholds in place — the mechanism by which the deployed model keeps learning
 * without being rebuilt or restarted.
 *
 * ProcessingFrequency: loop=2, epoch=10.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdUpdateSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private String faultFamily;
    private double threshold;
    private double offset;
    private long timestamp;

    public ThresholdUpdateSignal() {
        super();
        this.loop = 2;
        this.epoch = 10L;
        this.timeAlive = 0;
    }

    public ThresholdUpdateSignal(String faultFamily, double threshold, double offset, long timestamp) {
        this();
        this.faultFamily = faultFamily;
        this.threshold = threshold;
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public String getFaultFamily() { return faultFamily; }
    public void setFaultFamily(String faultFamily) { this.faultFamily = faultFamily; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public double getOffset() { return offset; }
    public void setOffset(double offset) { this.offset = offset; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ThresholdUpdateSignal.class; }
    @Override public String getDescription() { return "ThresholdUpdateSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ThresholdUpdateSignal c = new ThresholdUpdateSignal(faultFamily, threshold, offset, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
