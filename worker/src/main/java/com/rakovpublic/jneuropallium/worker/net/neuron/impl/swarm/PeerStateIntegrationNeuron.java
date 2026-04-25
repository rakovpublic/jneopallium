/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Layer 2 local-view of neighbours. Tracks last-seen tick + role per
 * peer; entries older than {@code stalenessTicks} are evicted.
 * Loop=2 / Epoch=1.
 */
public class PeerStateIntegrationNeuron extends ModulatableNeuron implements IPeerStateIntegrationNeuron {

    private final Map<String, Long> lastSeen = new HashMap<>();
    private final Map<String, AgentRole> roleByPeer = new HashMap<>();
    private long stalenessTicks = 300;

    public PeerStateIntegrationNeuron() { super(); }
    public PeerStateIntegrationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void onObservation(PeerObservationSignal o, long currentTick) {
        if (o == null || o.getPeerId() == null) return;
        lastSeen.put(o.getPeerId(), currentTick);
    }

    @Override
    public void onState(PeerStateSignal s, long currentTick) {
        if (s == null || s.getPeerId() == null) return;
        lastSeen.put(s.getPeerId(), currentTick);
        roleByPeer.put(s.getPeerId(), s.getRole());
    }

    @Override
    public int evict(long currentTick) {
        int n = 0;
        Iterator<Map.Entry<String, Long>> it = lastSeen.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (currentTick - e.getValue() > stalenessTicks) {
                it.remove();
                roleByPeer.remove(e.getKey());
                n++;
            }
        }
        return n;
    }

    @Override public Set<String> knownPeers() { return new HashSet<>(lastSeen.keySet()); }
    @Override public Map<String, AgentRole> roleSnapshot() { return Collections.unmodifiableMap(new HashMap<>(roleByPeer)); }
    @Override public Long lastSeen(String peerId) { return lastSeen.get(peerId); }
    @Override public void setStalenessTicks(long t) { this.stalenessTicks = Math.max(1L, t); }
    @Override public long getStalenessTicks() { return stalenessTicks; }
}
