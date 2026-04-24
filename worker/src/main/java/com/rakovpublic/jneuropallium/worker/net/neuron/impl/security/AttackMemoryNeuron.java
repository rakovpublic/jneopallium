/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Layer 3 attack-memory. Memoises campaign → TTP set mappings so
 * subsequent detection can raise priors rapidly (memory B-cell
 * analogue). Loop=2 / Epoch=1.
 */
public class AttackMemoryNeuron extends ModulatableNeuron implements IAttackMemoryNeuron {

    private static final class Campaign {
        final ThreatCategory category;
        final Set<String> ttps;
        Campaign(ThreatCategory c, Set<String> t) { this.category = c; this.ttps = t; }
    }

    private final Map<String, Campaign> byId = new HashMap<>();
    private final Map<String, Set<String>> ttpIndex = new HashMap<>();

    public AttackMemoryNeuron() { super(); }
    public AttackMemoryNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void store(String campaignId, ThreatCategory category, String... ttpSignatures) {
        if (campaignId == null || ttpSignatures == null) return;
        Set<String> ttps = new HashSet<>();
        for (String s : ttpSignatures) if (s != null) ttps.add(s);
        byId.put(campaignId, new Campaign(category, ttps));
        for (String s : ttps) ttpIndex.computeIfAbsent(s, k -> new HashSet<>()).add(campaignId);
    }

    @Override
    public Set<String> lookup(String... ttpSignatures) {
        Set<String> out = new HashSet<>();
        if (ttpSignatures == null) return out;
        for (String s : ttpSignatures) {
            if (s == null) continue;
            Set<String> hits = ttpIndex.get(s);
            if (hits != null) out.addAll(hits);
        }
        return out;
    }

    public ThreatCategory categoryOf(String campaignId) {
        Campaign c = byId.get(campaignId);
        return c == null ? null : c.category;
    }

    @Override public int size() { return byId.size(); }
    @Override public void clear() { byId.clear(); ttpIndex.clear(); }
}
