/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Slow context signal describing how the asset is being operated. The health
 * model uses it to avoid treating normal low-speed/high-load behavior as a
 * fault. ProcessingFrequency: loop=2, epoch=3.
 */
public class OperatingRegimeSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(3L, 2);

    private String assetId;
    private double rotationalSpeedRpm;
    private double loadFraction;
    private double flow;
    private double pressure;
    private double temperature;
    private double actuatorCommand;
    private String regimeName;
    private double confidence;
    private long timestamp;

    public OperatingRegimeSignal() {
        super();
        this.loop = 2;
        this.epoch = 3L;
        this.timeAlive = 1000;
        this.regimeName = "UNKNOWN";
    }

    public OperatingRegimeSignal(String assetId, double rotationalSpeedRpm, double loadFraction,
                                 double flow, double pressure, double temperature,
                                 double actuatorCommand, String regimeName, double confidence,
                                 long timestamp) {
        this();
        this.assetId = assetId;
        this.rotationalSpeedRpm = Math.max(0.0, rotationalSpeedRpm);
        this.loadFraction = clamp(loadFraction);
        this.flow = Math.max(0.0, flow);
        this.pressure = Math.max(0.0, pressure);
        this.temperature = temperature;
        this.actuatorCommand = clamp(actuatorCommand);
        this.regimeName = regimeName == null ? "UNKNOWN" : regimeName;
        this.confidence = clamp(confidence);
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public double getRotationalSpeedRpm() { return rotationalSpeedRpm; }
    public void setRotationalSpeedRpm(double rotationalSpeedRpm) {
        this.rotationalSpeedRpm = Math.max(0.0, rotationalSpeedRpm);
    }
    public double getLoadFraction() { return loadFraction; }
    public void setLoadFraction(double loadFraction) { this.loadFraction = clamp(loadFraction); }
    public double getFlow() { return flow; }
    public void setFlow(double flow) { this.flow = Math.max(0.0, flow); }
    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = Math.max(0.0, pressure); }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getActuatorCommand() { return actuatorCommand; }
    public void setActuatorCommand(double actuatorCommand) { this.actuatorCommand = clamp(actuatorCommand); }
    public String getRegimeName() { return regimeName; }
    public void setRegimeName(String regimeName) { this.regimeName = regimeName == null ? "UNKNOWN" : regimeName; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = clamp(confidence); }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return OperatingRegimeSignal.class; }
    @Override public String getDescription() { return "OperatingRegimeSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        OperatingRegimeSignal c = new OperatingRegimeSignal(assetId, rotationalSpeedRpm,
                loadFraction, flow, pressure, temperature, actuatorCommand, regimeName,
                confidence, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
