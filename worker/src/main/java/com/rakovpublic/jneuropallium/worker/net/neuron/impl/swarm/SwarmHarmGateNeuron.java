/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.SwarmAlertSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 5 collective harm aggregator. Sums per-agent projected
 * emissions in a region; emits a {@link SwarmAlertSignal} when the
 * sum exceeds the regional threshold. Loop=1 / Epoch=1.
 */
public class SwarmHarmGateNeuron extends ModulatableNeuron implements ISwarmHarmGateNeuron {

    private final Map<String, Map<String, Double>> regionalEmissions = new HashMap<>();
    private final Map<String, Double> thresholds = new HashMap<>();

    public SwarmHarmGateNeuron() { super(); }
    public SwarmHarmGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void recordEmission(String regionId, String agentId, double projectedEmission) {
        if (regionId == null || agentId == null) return;
        regionalEmissions.computeIfAbsent(regionId, k -> new HashMap<>())
                .put(agentId, Math.max(0.0, projectedEmission));
    }

    @Override
    public SwarmAlertSignal aggregate(String regionId) {
        if (regionId == null) return null;
        double total = currentEmission(regionId);
        Double thr = thresholds.get(regionId);
        if (thr == null || total <= thr) return null;
        double severity = Math.min(1.0, (total - thr) / Math.max(1e-6, thr));
        return new SwarmAlertSignal(AlertCategory.COLLECTIVE_HARM, regionId, severity);
    }

    @Override public void setRegionalThreshold(String regionId, double threshold) {
        if (regionId != null) thresholds.put(regionId, Math.max(0.0, threshold));
    }

    @Override
    public double currentEmission(String regionId) {
        Map<String, Double> m = regionalEmissions.get(regionId);
        if (m == null) return 0.0;
        double sum = 0.0;
        for (Double v : m.values()) sum += v;
        return sum;
    }

    @Override
    public double tighteningMultiplier(String regionId) {
        Double thr = thresholds.get(regionId);
        if (thr == null || thr <= 0) return 1.0;
        double total = currentEmission(regionId);
        if (total <= thr) return 1.0;
        return Math.min(5.0, total / thr);
    }
}
