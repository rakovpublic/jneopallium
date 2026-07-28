/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Layer 2 ISA-18.2 alarm aggregator. Suppresses standing alarms (same
 * tag + code within the suppression window) and tracks the global
 * alarm rate. Loop=1 / Epoch=2.
 */
public class AlarmAggregationNeuron extends ModulatableNeuron implements IAlarmAggregationNeuron {

    private final Map<String, Long> lastSeen = new HashMap<>();
    private final Deque<Long> recent = new ArrayDeque<>();
    private long suppressionWindowTicks = 600;
    private int forwarded;

    public AlarmAggregationNeuron() { super(); }
    public AlarmAggregationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setSuppressionWindowTicks(long t) { this.suppressionWindowTicks = Math.max(0L, t); }

    @Override
    public AlarmSignal observe(AlarmSignal a) {
        if (a == null) return null;
        String key = (a.getTag() == null ? "" : a.getTag()) + '|'
                + (a.getConditionCode() == null ? "" : a.getConditionCode());
        long now = a.getTimestamp();
        Long last = lastSeen.get(key);
        if (last != null && now - last < suppressionWindowTicks) {
            return null; // standing alarm suppression
        }
        lastSeen.put(key, now);
        recent.addLast(now);
        while (!recent.isEmpty() && now - recent.peekFirst() > 60_000L) recent.removeFirst();
        forwarded++;
        return a;
    }

    @Override public int standingAlarmCount() { return lastSeen.size(); }
    @Override public double alarmRatePerMin() { return recent.size(); }
}
