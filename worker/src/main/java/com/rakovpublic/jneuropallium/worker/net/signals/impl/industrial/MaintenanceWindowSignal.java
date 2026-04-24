/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Scheduled maintenance window for an asset. ProcessingFrequency:
 * loop=2, epoch=10.
 */
public class MaintenanceWindowSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private String assetId;
    private long scheduledTick;
    private int durationTicks;

    public MaintenanceWindowSignal() { super(); this.loop = 2; this.epoch = 10L; this.timeAlive = 100_000; }

    public MaintenanceWindowSignal(String assetId, long scheduledTick, int durationTicks) {
        this();
        this.assetId = assetId;
        this.scheduledTick = scheduledTick;
        this.durationTicks = Math.max(1, durationTicks);
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String a) { this.assetId = a; }
    public long getScheduledTick() { return scheduledTick; }
    public void setScheduledTick(long s) { this.scheduledTick = s; }
    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int d) { this.durationTicks = Math.max(1, d); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MaintenanceWindowSignal.class; }
    @Override public String getDescription() { return "MaintenanceWindowSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        MaintenanceWindowSignal c = new MaintenanceWindowSignal(assetId, scheduledTick, durationTicks);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
