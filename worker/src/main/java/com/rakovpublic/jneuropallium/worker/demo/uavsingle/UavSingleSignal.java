package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UavSingleSignal extends AbstractSignal<Void> implements IInputSignal<Void>, IResultSignal<Void> {
    private String missionId;
    private String uavId;
    private long tick;
    private String eventType;
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public UavSingleSignal() {
        super();
        this.timeAlive = 100;
        this.epoch = 1L;
        this.loop = 1;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getUavId() {
        return uavId;
    }

    public void setUavId(String uavId) {
        this.uavId = uavId;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attributes);
    }

    public UavSingleSignal attribute(String key, Object value) {
        attributes.put(key, value);
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
        return eventType == null ? getClass().getSimpleName() : eventType;
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
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        try {
            UavSingleSignal copy = getClass().getDeclaredConstructor().newInstance();
            copyInto(copy);
            return (K) copy;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot copy UAV single signal " + getClass().getName(), e);
        }
    }

    protected void copyInto(UavSingleSignal copy) {
        copy.missionId = missionId;
        copy.uavId = uavId;
        copy.tick = tick;
        copy.eventType = eventType;
        copy.attributes = new LinkedHashMap<>(attributes);
        copy.sourceLayer = sourceLayer;
        copy.sourceNeuron = sourceNeuron;
        copy.timeAlive = timeAlive;
        copy.epoch = epoch;
        copy.loop = loop;
        copy.name = name;
        copy.inputName = inputName;
    }
}

