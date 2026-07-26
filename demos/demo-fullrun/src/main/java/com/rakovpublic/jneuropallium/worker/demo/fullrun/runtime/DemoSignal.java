package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemoSignal extends AbstractSignal<Void> implements IInputSignal<Void>, IResultSignal<Void> {
    private String demoId;
    private long tick;
    private String entityId;
    private String signalType;
    private String resultType;
    private double value;
    private double confidence;
    private String mode;
    private String decision;
    private String reason;
    private Map<String, String> attributes = new LinkedHashMap<>();

    public DemoSignal() {
        super();
        this.timeAlive = 0;
        this.epoch = 1L;
        this.loop = 1;
        this.confidence = 1.0;
    }

    public String getDemoId() {
        return demoId;
    }

    public void setDemoId(String demoId) {
        this.demoId = demoId;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public double getNumericValue() {
        return value;
    }

    public void setNumericValue(double value) {
        this.value = value;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public DemoSignal withAttribute(String key, Object value) {
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
        return signalType == null ? getClass().getSimpleName() : signalType;
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
            DemoSignal copy = getClass().getDeclaredConstructor().newInstance();
            copyInto(copy);
            return (K) copy;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot copy demo signal " + getClass().getName(), e);
        }
    }

    protected void copyInto(DemoSignal copy) {
        copy.demoId = demoId;
        copy.tick = tick;
        copy.entityId = entityId;
        copy.signalType = signalType;
        copy.resultType = resultType;
        copy.value = value;
        copy.confidence = confidence;
        copy.mode = mode;
        copy.decision = decision;
        copy.reason = reason;
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
    }
}
