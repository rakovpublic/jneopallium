/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.security;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.security.LogLevel;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalised log event from any audit source (syslog / Windows Event /
 * CloudTrail-style).
 * ProcessingFrequency: loop=1, epoch=2.
 */
public class LogEventSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private String source;
    private LogLevel level;
    private Map<String, String> fields;
    private long timestamp;

    public LogEventSignal() {
        super();
        this.loop = 1;
        this.epoch = 2L;
        this.timeAlive = 200;
        this.level = LogLevel.INFO;
        this.fields = new HashMap<>();
    }

    public LogEventSignal(String source, LogLevel level, Map<String, String> fields, long timestamp) {
        this();
        this.source = source;
        this.level = level == null ? LogLevel.INFO : level;
        this.fields = fields == null ? new HashMap<>() : new HashMap<>(fields);
        this.timestamp = timestamp;
    }

    public String getSource() { return source; }
    public void setSource(String s) { this.source = s; }
    public LogLevel getLevel() { return level; }
    public void setLevel(LogLevel l) { this.level = l == null ? LogLevel.INFO : l; }
    public Map<String, String> getFields() { return Collections.unmodifiableMap(fields); }
    public void setFields(Map<String, String> f) { this.fields = f == null ? new HashMap<>() : new HashMap<>(f); }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LogEventSignal.class; }
    @Override public String getDescription() { return "LogEventSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        LogEventSignal c = new LogEventSignal(source, level, fields, timestamp);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
