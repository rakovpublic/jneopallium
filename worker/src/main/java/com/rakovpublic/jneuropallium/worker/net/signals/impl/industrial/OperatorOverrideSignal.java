/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.OverrideKind;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Operator override. Per spec §5 operator override always wins for
 * regulatory control; it does not affect interlocks. ProcessingFrequency:
 * loop=1, epoch=1.
 */
public class OperatorOverrideSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String tag;
    private OverrideKind kind;
    private String operatorId;
    private String reason;
    private double manualValue;

    public OperatorOverrideSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 30;
        this.kind = OverrideKind.MANUAL;
    }

    public OperatorOverrideSignal(String tag, OverrideKind kind, String operatorId, String reason, double manualValue) {
        this();
        this.tag = tag;
        this.kind = kind == null ? OverrideKind.MANUAL : kind;
        this.operatorId = operatorId;
        this.reason = reason;
        this.manualValue = manualValue;
    }

    public String getTag() { return tag; }
    public void setTag(String t) { this.tag = t; }
    public OverrideKind getKind() { return kind; }
    public void setKind(OverrideKind k) { this.kind = k == null ? OverrideKind.MANUAL : k; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String o) { this.operatorId = o; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public double getManualValue() { return manualValue; }
    public void setManualValue(double v) { this.manualValue = v; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return OperatorOverrideSignal.class; }
    @Override public String getDescription() { return "OperatorOverrideSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        OperatorOverrideSignal c = new OperatorOverrideSignal(tag, kind, operatorId, reason, manualValue);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
