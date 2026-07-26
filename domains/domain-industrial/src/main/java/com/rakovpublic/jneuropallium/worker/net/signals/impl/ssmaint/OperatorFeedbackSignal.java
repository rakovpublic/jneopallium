/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * An operator's verdict on a previously emitted advisory: confirmed (maintenance
 * genuinely needed) or a false positive. This is the only supervision the model
 * ever receives and it arrives while running — it drives the continuous online
 * adaptation without any redeploy. {@code domainShift} carries the shift score
 * observed when the advisory fired so the adapter can freeze during novelty.
 *
 * ProcessingFrequency: loop=2, epoch=10 (slow loop; feedback is infrequent).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperatorFeedbackSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private String assetId;
    private String faultFamily;
    private boolean confirmed;
    private double domainShift;
    private String operator;
    private long timestamp;

    public OperatorFeedbackSignal() {
        super();
        this.loop = 2;
        this.epoch = 10L;
        this.timeAlive = 0;
    }

    public OperatorFeedbackSignal(String assetId, String faultFamily, boolean confirmed,
                                  double domainShift, String operator, long timestamp) {
        this();
        this.assetId = assetId;
        this.faultFamily = faultFamily;
        this.confirmed = confirmed;
        this.domainShift = domainShift;
        this.operator = operator;
        this.timestamp = timestamp;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getFaultFamily() { return faultFamily; }
    public void setFaultFamily(String faultFamily) { this.faultFamily = faultFamily; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public double getDomainShift() { return domainShift; }
    public void setDomainShift(double domainShift) { this.domainShift = domainShift; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return OperatorFeedbackSignal.class; }
    @Override public String getDescription() { return "OperatorFeedbackSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        OperatorFeedbackSignal c = new OperatorFeedbackSignal(assetId, faultFamily, confirmed,
                domainShift, operator, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
