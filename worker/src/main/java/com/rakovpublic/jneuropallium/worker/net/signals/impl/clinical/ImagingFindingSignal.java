/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.FindingCategory;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * A radiologist- or model-reported imaging finding.
 * ProcessingFrequency: loop=2, epoch=5.
 */
public class ImagingFindingSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private String modality;     // CT, MRI, X-RAY, US, PET
    private String regionCode;   // BodyPart code
    private FindingCategory category;
    private double confidence;
    private String patientId;

    public ImagingFindingSignal() {
        super();
        this.loop = 2;
        this.epoch = 5L;
        this.timeAlive = 2000;
        this.category = FindingCategory.INDETERMINATE;
    }

    public ImagingFindingSignal(String modality, String regionCode,
                                FindingCategory category, double confidence, String patientId) {
        this();
        this.modality = modality;
        this.regionCode = regionCode;
        this.category = category == null ? FindingCategory.INDETERMINATE : category;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.patientId = patientId;
    }

    public String getModality() { return modality; }
    public void setModality(String m) { this.modality = m; }
    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String r) { this.regionCode = r; }
    public FindingCategory getCategory() { return category; }
    public void setCategory(FindingCategory c) { this.category = c == null ? FindingCategory.INDETERMINATE : c; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double c) { this.confidence = Math.max(0.0, Math.min(1.0, c)); }
    public String getPatientId() { return patientId; }
    public void setPatientId(String p) { this.patientId = p; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ImagingFindingSignal.class; }
    @Override public String getDescription() { return "ImagingFindingSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        ImagingFindingSignal c = new ImagingFindingSignal(modality, regionCode, category, confidence, patientId);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
