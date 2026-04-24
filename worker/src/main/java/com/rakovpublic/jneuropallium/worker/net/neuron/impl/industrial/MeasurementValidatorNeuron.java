/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 0 range / rate-of-change filter. Downgrades quality to
 * UNCERTAIN on anomalies rather than dropping the reading — a dropped
 * measurement is worse than a flagged one in industrial control.
 * Loop=1 / Epoch=1.
 */
public class MeasurementValidatorNeuron extends ModulatableNeuron implements IMeasurementValidatorNeuron {

    private static final class Range { double min, max; Range(double l, double h){min=l;max=h;} }
    private static final class Rate { double maxAbsPerSec; Rate(double r){maxAbsPerSec=r;} }
    private static final class Last { double value; long tsMs; Last(double v,long t){value=v;tsMs=t;} }

    private final Map<String, Range> ranges = new HashMap<>();
    private final Map<String, Rate> rates = new HashMap<>();
    private final Map<String, Last> lastSample = new HashMap<>();
    private int suspicious;

    public MeasurementValidatorNeuron() { super(); }
    public MeasurementValidatorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setRange(String tag, double min, double max) {
        if (tag != null) ranges.put(tag, new Range(Math.min(min, max), Math.max(min, max)));
    }
    @Override public void setMaxRateOfChange(String tag, double unitsPerSecond) {
        if (tag != null) rates.put(tag, new Rate(Math.max(0.0, unitsPerSecond)));
    }

    @Override
    public MeasurementSignal validate(MeasurementSignal m) {
        if (m == null || m.getTag() == null) return m;
        boolean bad = false;
        Range r = ranges.get(m.getTag());
        if (r != null && (m.getMeasurement() < r.min || m.getMeasurement() > r.max)) bad = true;
        Rate rate = rates.get(m.getTag());
        Last last = lastSample.get(m.getTag());
        if (!bad && rate != null && last != null && m.getTimestamp() > last.tsMs) {
            double dt = (m.getTimestamp() - last.tsMs) / 1000.0;
            double slope = Math.abs(m.getMeasurement() - last.value) / Math.max(1e-6, dt);
            if (slope > rate.maxAbsPerSec) bad = true;
        }
        if (bad) {
            m.setQuality(Quality.UNCERTAIN);
            suspicious++;
        }
        lastSample.put(m.getTag(), new Last(m.getMeasurement(), m.getTimestamp()));
        return m;
    }

    @Override public int suspiciousCount() { return suspicious; }
}
