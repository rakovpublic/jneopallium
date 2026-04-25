/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Layer 2 role-distribution awareness. Maintains an EnumMap of how
 * many peers are in each role and exposes the under-represented role
 * the agent should bias its specialisation toward. Loop=2 / Epoch=2.
 */
public class RoleAwarenessNeuron extends ModulatableNeuron implements IRoleAwarenessNeuron {

    private AgentRole ownRole = AgentRole.IDLE;
    private final Map<String, AgentRole> peers = new HashMap<>();

    public RoleAwarenessNeuron() { super(); }
    public RoleAwarenessNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public AgentRole ownRole() { return ownRole; }
    @Override public void setOwnRole(AgentRole r) { this.ownRole = r == null ? AgentRole.IDLE : r; }

    @Override public void onPeerState(PeerStateSignal s) {
        if (s == null || s.getPeerId() == null) return;
        peers.put(s.getPeerId(), s.getRole());
    }

    @Override
    public Map<AgentRole, Integer> distribution() {
        EnumMap<AgentRole, Integer> dist = new EnumMap<>(AgentRole.class);
        for (AgentRole r : AgentRole.values()) dist.put(r, 0);
        for (AgentRole r : peers.values()) dist.merge(r, 1, Integer::sum);
        return Collections.unmodifiableMap(dist);
    }

    @Override
    public AgentRole shortageRole() {
        Map<AgentRole, Integer> d = distribution();
        AgentRole minRole = null;
        int minCount = Integer.MAX_VALUE;
        for (AgentRole r : AgentRole.values()) {
            if (r == AgentRole.IDLE) continue;
            int c = d.getOrDefault(r, 0);
            if (c < minCount) { minCount = c; minRole = r; }
        }
        return minRole;
    }
}
