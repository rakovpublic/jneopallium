/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Layer 2 lateral-movement detector. Counts the number of distinct
 * hosts a single user has authenticated against; once it exceeds the
 * fanout threshold the neuron emits a beacon-like anomaly score.
 * Loop=1 / Epoch=3.
 */
public class LateralMovementNeuron extends ModulatableNeuron implements ILateralMovementNeuron {

    private final Map<String, Set<String>> fanout = new HashMap<>();
    private int fanoutThreshold = 5;

    public LateralMovementNeuron() { super(); }
    public LateralMovementNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public AnomalyScoreSignal recordAuth(String user, String host, long tick) {
        if (user == null || host == null) return null;
        Set<String> hosts = fanout.computeIfAbsent(user, k -> new HashSet<>());
        hosts.add(host);
        if (hosts.size() >= fanoutThreshold) {
            double score = Math.min(1.0, (double) hosts.size() / (2.0 * fanoutThreshold));
            ArrayList<String> features = new ArrayList<>();
            features.add("fanout=" + hosts.size());
            return new AnomalyScoreSignal(user, score, features);
        }
        return null;
    }

    @Override
    public int hostsFor(String user) {
        Set<String> h = fanout.get(user);
        return h == null ? 0 : h.size();
    }

    @Override public void setFanoutThreshold(int t) { this.fanoutThreshold = Math.max(2, t); }
    @Override public int getFanoutThreshold() { return fanoutThreshold; }
}
