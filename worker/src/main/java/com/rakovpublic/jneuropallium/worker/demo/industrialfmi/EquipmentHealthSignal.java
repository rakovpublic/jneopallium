/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/** Slow-loop health estimate for P-101. */
public final class EquipmentHealthSignal extends AbstractSignal<Void> implements ISignal<Void>, IInputSignal<Void> {
    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private String assetId;
    private double vibrationRms;
    private double bearingTemperature;
    private double pumpPowerKw;
    private double risk;
    private long timestamp;

    public EquipmentHealthSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 20;
    }

    public EquipmentHealthSignal(String assetId, double vibrationRms, double bearingTemperature,
                                 double pumpPowerKw, double risk, long timestamp) {
        this();
        this.assetId = assetId;
        this.vibrationRms = vibrationRms;
        this.bearingTemperature = bearingTemperature;
        this.pumpPowerKw = pumpPowerKw;
        this.risk = risk;
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public double getVibrationRms() { return vibrationRms; }
    public double getBearingTemperature() { return bearingTemperature; }
    public double getPumpPowerKw() { return pumpPowerKw; }
    public double getRisk() { return risk; }
    public long getTimestamp() { return timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return EquipmentHealthSignal.class; }
    @Override public String getDescription() { return "EquipmentHealthSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        EquipmentHealthSignal c = new EquipmentHealthSignal(assetId, vibrationRms, bearingTemperature,
                pumpPowerKw, risk, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
