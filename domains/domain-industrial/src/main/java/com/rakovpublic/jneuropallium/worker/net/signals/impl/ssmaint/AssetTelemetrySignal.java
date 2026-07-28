/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One multi-sensor telemetry frame for a single asset at a single tick. This is
 * the only raw input the label-free maintenance model consumes. It carries the
 * named sensor channels plus the current operating regime so downstream neurons
 * can judge a reading relative to its load condition.
 *
 * ProcessingFrequency: loop=1, epoch=1 (fast loop, every tick).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetTelemetrySignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String assetId;
    private int regime;
    private Map<String, Double> sensors;
    private long timestamp;

    public AssetTelemetrySignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 0;
        this.sensors = new LinkedHashMap<>();
    }

    public AssetTelemetrySignal(String assetId, int regime, Map<String, Double> sensors, long timestamp) {
        this();
        this.assetId = assetId;
        this.regime = regime;
        setSensors(sensors);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public int getRegime() { return regime; }
    public void setRegime(int regime) { this.regime = regime; }
    public Map<String, Double> getSensors() { return Collections.unmodifiableMap(sensors); }
    public void setSensors(Map<String, Double> sensors) {
        this.sensors = new LinkedHashMap<>();
        if (sensors == null) return;
        for (Map.Entry<String, Double> e : sensors.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && Double.isFinite(e.getValue())) {
                this.sensors.put(e.getKey(), e.getValue());
            }
        }
    }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AssetTelemetrySignal.class; }
    @Override public String getDescription() { return "AssetTelemetrySignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AssetTelemetrySignal c = new AssetTelemetrySignal(assetId, regime, sensors, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
