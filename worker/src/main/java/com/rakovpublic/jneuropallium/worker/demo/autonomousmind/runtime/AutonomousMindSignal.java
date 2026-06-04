package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousMindSignal extends AbstractSignal<Void> implements IInputSignal<Void>, IResultSignal<Void> {
    private String scenario;
    private long tick;
    private String modality;
    private String sourceId;
    private String signalType;
    private String system;
    private String payloadSummary;
    private double confidence = 1.0;
    private double noiseEstimate;
    private String calibrationStatus = "OK";
    private String sourceHealth = "OK";
    private String position;
    private String orientation;
    private String processingFrequency = "fast";
    private Map<String, String> attributes = new LinkedHashMap<>();

    public AutonomousMindSignal() {
        super();
        this.timeAlive = 0;
        this.epoch = 1L;
        this.loop = 1;
        this.innerLoop = 1;
        this.description = "AutonomousMindSignal";
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
        this.description = signalType;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getPayloadSummary() {
        return payloadSummary;
    }

    public void setPayloadSummary(String payloadSummary) {
        this.payloadSummary = payloadSummary;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getNoiseEstimate() {
        return noiseEstimate;
    }

    public void setNoiseEstimate(double noiseEstimate) {
        this.noiseEstimate = noiseEstimate;
    }

    public String getCalibrationStatus() {
        return calibrationStatus;
    }

    public void setCalibrationStatus(String calibrationStatus) {
        this.calibrationStatus = calibrationStatus;
    }

    public String getSourceHealth() {
        return sourceHealth;
    }

    public void setSourceHealth(String sourceHealth) {
        this.sourceHealth = sourceHealth;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getProcessingFrequency() {
        return processingFrequency;
    }

    public void setProcessingFrequency(String processingFrequency) {
        this.processingFrequency = processingFrequency;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public AutonomousMindSignal withAttribute(String key, Object value) {
        attributes.put(key, value == null ? "" : String.valueOf(value));
        return this;
    }

    @Override
    public Void getValue() {
        return null;
    }

    @Override
    public Class<Void> getParamClass() {
        return Void.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ISignal<Void>> getCurrentSignalClass() {
        return (Class<? extends ISignal<Void>>) getClass();
    }

    @Override
    public String getDescription() {
        return signalType == null ? "AutonomousMindSignal" : signalType;
    }

    @Override
    public Void getResultObject() {
        return null;
    }

    @Override
    public Class<Void> getResultObjectClass() {
        return Void.class;
    }

    @Override
    public String toJSON() {
        return attributes.toString();
    }

    @Override
    public int getSourceLayerId() {
        return sourceLayer == null ? -1 : sourceLayer;
    }

    @Override
    public Long getSourceNeuronId() {
        return sourceNeuron;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        try {
            AutonomousMindSignal copy = getClass().getDeclaredConstructor().newInstance();
            copyInto(copy);
            return (K) copy;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("Cannot copy AutonomousMind signal", e);
        }
    }

    protected void copyInto(AutonomousMindSignal copy) {
        copy.scenario = scenario;
        copy.tick = tick;
        copy.modality = modality;
        copy.sourceId = sourceId;
        copy.signalType = signalType;
        copy.system = system;
        copy.payloadSummary = payloadSummary;
        copy.confidence = confidence;
        copy.noiseEstimate = noiseEstimate;
        copy.calibrationStatus = calibrationStatus;
        copy.sourceHealth = sourceHealth;
        copy.position = position;
        copy.orientation = orientation;
        copy.processingFrequency = processingFrequency;
        copy.attributes = new LinkedHashMap<>(attributes);
        copy.sourceLayer = sourceLayer;
        copy.sourceNeuron = sourceNeuron;
        copy.currentInnerLoop = currentInnerLoop;
        copy.epoch = epoch;
        copy.innerLoop = innerLoop;
        copy.loop = loop;
        copy.name = name;
        copy.inputName = inputName;
        copy.timeAlive = timeAlive;
        copy.description = description;
    }
}
