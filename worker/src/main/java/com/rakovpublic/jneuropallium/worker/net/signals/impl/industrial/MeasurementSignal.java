/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A field measurement read from a sensor. {@code tag} is an ISA-95
 * reference (process-area/unit/loop). {@code timestamp} is wall-clock
 * in epoch-millis; per spec §3 industrial compliance requires this in
 * addition to the jneopallium tick.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class MeasurementSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String tag;
    private double value;
    private Quality quality;
    private long timestamp;

    public MeasurementSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.quality = Quality.GOOD;
    }

    public MeasurementSignal(String tag, double value, Quality quality, long timestamp) {
        this();
        this.tag = tag;
        this.value = value;
        this.quality = quality == null ? Quality.UNCERTAIN : quality;
        this.timestamp = timestamp;
    }

    public String getTag() { return tag; }
    public void setTag(String t) { this.tag = t; }
    public double getMeasurement() { return value; }
    public void setMeasurement(double v) { this.value = v; }
    public Quality getQuality() { return quality; }
    public void setQuality(Quality q) { this.quality = q == null ? Quality.UNCERTAIN : q; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MeasurementSignal.class; }
    @Override public String getDescription() { return "MeasurementSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MeasurementSignal c = new MeasurementSignal(tag, value, quality, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
