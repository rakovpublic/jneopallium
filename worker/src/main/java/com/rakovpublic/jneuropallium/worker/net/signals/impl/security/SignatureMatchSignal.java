/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Output of a signature / YARA / Snort-style match with its supporting
 * IoC reference.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class SignatureMatchSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private String signatureId;
    private String family;
    private double confidence;
    private String referenceIoc;

    public SignatureMatchSignal() { super(); this.loop = 1; this.epoch = 1L; this.timeAlive = 50; }

    public SignatureMatchSignal(String signatureId, String family, double confidence, String referenceIoc) {
        this();
        this.signatureId = signatureId;
        this.family = family;
        this.confidence = clamp01(confidence);
        this.referenceIoc = referenceIoc;
    }

    public String getSignatureId() { return signatureId; }
    public void setSignatureId(String s) { this.signatureId = s; }
    public String getFamily() { return family; }
    public void setFamily(String f) { this.family = f; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = clamp01(c); }
    public String getReferenceIoc() { return referenceIoc; }
    public void setReferenceIoc(String r) { this.referenceIoc = r; }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return SignatureMatchSignal.class; }
    @Override public String getDescription() { return "SignatureMatchSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        SignatureMatchSignal c = new SignatureMatchSignal(signatureId, family, confidence, referenceIoc);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
