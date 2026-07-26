package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutonomousAiSignal extends AbstractSignal<Void> implements IInputSignal<Void>, IResultSignal<Void> {
    private String scenarioId;
    private long tick;
    private String stage;
    private String layerName;
    private String neuronLabel;
    private String signalType;
    private String resultType;
    private double numericValue;
    private Map<String, String> attributes = new LinkedHashMap<>();

    public AutonomousAiSignal() {
        super();
        this.timeAlive = 0;
        this.epoch = 1L;
        this.loop = 1;
        this.innerLoop = 1;
        this.description = "autonomous-ai-signal";
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
        this.description = stage;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getNeuronLabel() {
        return neuronLabel;
    }

    public void setNeuronLabel(String neuronLabel) {
        this.neuronLabel = neuronLabel;
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
        return numericValue;
    }

    public void setNumericValue(double numericValue) {
        this.numericValue = numericValue;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public AutonomousAiSignal withAttribute(String key, Object value) {
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
        return stage == null ? "autonomous-ai-signal" : stage;
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
            AutonomousAiSignal copy = getClass().getDeclaredConstructor().newInstance();
            copyInto(copy);
            return (K) copy;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new IllegalStateException("Cannot copy autonomous AI signal", e);
        }
    }

    protected void copyInto(AutonomousAiSignal copy) {
        copy.scenarioId = scenarioId;
        copy.tick = tick;
        copy.stage = stage;
        copy.layerName = layerName;
        copy.neuronLabel = neuronLabel;
        copy.signalType = signalType;
        copy.resultType = resultType;
        copy.numericValue = numericValue;
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
