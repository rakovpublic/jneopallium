package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SpikeSignal extends BaseSignal {
    private boolean fired;
    private double magnitude;
    private int burstCount;

    public SpikeSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public SpikeSignal(boolean fired, double magnitude, int burstCount) {
        this(); this.fired = fired; this.magnitude = magnitude; this.burstCount = burstCount;
    }

    public boolean isFired() { return fired; }
    public void setFired(boolean fired) { this.fired = fired; }
    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }
    public int getBurstCount() { return burstCount; }
    public void setBurstCount(int burstCount) { this.burstCount = burstCount; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SpikeSignal.class; }
    @Override public String getDescription() { return "SpikeSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        SpikeSignal c = new SpikeSignal(fired, magnitude, burstCount);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
