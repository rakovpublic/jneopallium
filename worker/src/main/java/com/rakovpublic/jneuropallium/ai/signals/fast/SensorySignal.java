package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SensorySignal extends BaseSignal {
    private double[] rawValues;
    private String sensorId;
    private long timestamp;

    public SensorySignal() { super(); this.loop = 1; this.epoch = 1L; }
    public SensorySignal(double[] rawValues, String sensorId, long timestamp) {
        this(); this.rawValues = rawValues; this.sensorId = sensorId; this.timestamp = timestamp;
    }

    public double[] getRawValues() { return rawValues; }
    public void setRawValues(double[] rawValues) { this.rawValues = rawValues; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SensorySignal.class; }
    @Override public String getDescription() { return "SensorySignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        SensorySignal c = new SensorySignal(rawValues, sensorId, timestamp);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
