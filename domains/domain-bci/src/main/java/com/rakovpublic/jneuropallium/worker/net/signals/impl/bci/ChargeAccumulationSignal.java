/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Running charge accumulation at an electrode, for charge-balance and
 * Shannon-criterion monitoring.
 * ProcessingFrequency: loop=2, epoch=1.
 */
public class ChargeAccumulationSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 2);

    private int electrodeId;
    private double netChargeUC;
    private double perPhaseChargeDensityUCm2;

    public ChargeAccumulationSignal() {
        super();
        this.loop = 2;
        this.epoch = 1L;
        this.timeAlive = 100;
    }

    public ChargeAccumulationSignal(int electrodeId, double netChargeUC, double perPhaseChargeDensityUCm2) {
        this();
        this.electrodeId = electrodeId;
        this.netChargeUC = netChargeUC;
        this.perPhaseChargeDensityUCm2 = perPhaseChargeDensityUCm2;
    }

    public int getElectrodeId() { return electrodeId; }
    public void setElectrodeId(int e) { this.electrodeId = e; }
    public double getNetChargeUC() { return netChargeUC; }
    public void setNetChargeUC(double v) { this.netChargeUC = v; }
    public double getPerPhaseChargeDensityUCm2() { return perPhaseChargeDensityUCm2; }
    public void setPerPhaseChargeDensityUCm2(double v) { this.perPhaseChargeDensityUCm2 = v; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ChargeAccumulationSignal.class; }
    @Override public String getDescription() { return "ChargeAccumulationSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ChargeAccumulationSignal c = new ChargeAccumulationSignal(electrodeId, netChargeUC, perPhaseChargeDensityUCm2);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
