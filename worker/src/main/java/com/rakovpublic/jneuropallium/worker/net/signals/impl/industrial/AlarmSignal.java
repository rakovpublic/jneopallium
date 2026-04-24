/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * ISA-18.2 alarm with a priority and a condition code.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class AlarmSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private AlarmPriority priority;
    private String tag;
    private String conditionCode;
    private long timestamp;

    public AlarmSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 100;
        this.priority = AlarmPriority.JOURNAL;
    }

    public AlarmSignal(AlarmPriority priority, String tag, String conditionCode, long timestamp) {
        this();
        this.priority = priority == null ? AlarmPriority.JOURNAL : priority;
        this.tag = tag;
        this.conditionCode = conditionCode;
        this.timestamp = timestamp;
    }

    public AlarmPriority getPriority() { return priority; }
    public void setPriority(AlarmPriority p) { this.priority = p == null ? AlarmPriority.JOURNAL : p; }
    public String getTag() { return tag; }
    public void setTag(String t) { this.tag = t; }
    public String getConditionCode() { return conditionCode; }
    public void setConditionCode(String c) { this.conditionCode = c; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AlarmSignal.class; }
    @Override public String getDescription() { return "AlarmSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        AlarmSignal c = new AlarmSignal(priority, tag, conditionCode, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
