/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Layer 5 Byzantine-tolerance isolation. Quarantines a peer only after
 * {@code witnessThreshold} independent reports. Isolation is
 * time-bounded — repeats double the duration but never permanently
 * isolate locally. Loop=1 / Epoch=1.
 */
public class IsolationProtocolNeuron extends ModulatableNeuron implements IIsolationProtocolNeuron {

    private final Map<String, Set<String>> witnesses = new HashMap<>();
    private final Map<String, Long> isolatedUntil = new HashMap<>();
    private final Map<String, Integer> escalations = new HashMap<>();
    private int witnessThreshold = 3;
    private long isolationTicks = 600;

    public IsolationProtocolNeuron() { super(); }
    public IsolationProtocolNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setWitnessThreshold(int k) { this.witnessThreshold = Math.max(1, k); }
    @Override public int getWitnessThreshold() { return witnessThreshold; }
    @Override public void setIsolationTicks(long t) { this.isolationTicks = Math.max(1L, t); }
    @Override public long getIsolationTicks() { return isolationTicks; }

    @Override
    public boolean onReport(AgentAnomalySignal report, long currentTick) {
        if (report == null || report.getSuspectId() == null) return false;
        Set<String> w = witnesses.computeIfAbsent(report.getSuspectId(), k -> new HashSet<>());
        if (report.getDetectorId() != null) w.add(report.getDetectorId());
        for (String wn : report.getWitnesses()) if (wn != null) w.add(wn);
        if (w.size() < witnessThreshold) return false;
        int prev = escalations.getOrDefault(report.getSuspectId(), 0);
        long mult = 1L << Math.min(prev, 8);
        isolatedUntil.put(report.getSuspectId(), currentTick + isolationTicks * mult);
        escalations.put(report.getSuspectId(), prev + 1);
        return true;
    }

    @Override
    public boolean isIsolated(String peerId, long currentTick) {
        Long u = isolatedUntil.get(peerId);
        return u != null && u > currentTick;
    }

    @Override
    public int tick(long currentTick) {
        int n = 0;
        Iterator<Map.Entry<String, Long>> it = isolatedUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= currentTick) {
                it.remove();
                witnesses.remove(e.getKey());
                n++;
            }
        }
        return n;
    }

    @Override
    public int activeIsolations(long currentTick) {
        int n = 0;
        for (Long u : isolatedUntil.values()) if (u > currentTick) n++;
        return n;
    }
}
