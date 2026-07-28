/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.AgentAnomalySignal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Layer 5 anomaly reporter. Loop=2 / Epoch=1. */
public class AnomalyReportNeuron extends ModulatableNeuron implements IAnomalyReportNeuron {

    private String selfId;
    private final Map<String, Integer> reportsBySuspect = new HashMap<>();
    private final Set<String> alreadyReportedBySelf = new HashSet<>();

    public AnomalyReportNeuron() { super(); }
    public AnomalyReportNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setSelfId(String id) { this.selfId = id; }
    @Override public String getSelfId() { return selfId; }

    @Override
    public AgentAnomalySignal report(String suspectId, AnomalyKind kind) {
        if (suspectId == null) return null;
        if (alreadyReportedBySelf.contains(suspectId)) return null;
        alreadyReportedBySelf.add(suspectId);
        reportsBySuspect.merge(suspectId, 1, Integer::sum);
        return new AgentAnomalySignal(suspectId, kind, selfId,
                Collections.singletonList(selfId == null ? "self" : selfId));
    }

    @Override public int reportsAgainst(String suspectId) { return reportsBySuspect.getOrDefault(suspectId, 0); }
    @Override public boolean alreadyReported(String suspectId) { return alreadyReportedBySelf.contains(suspectId); }
}
