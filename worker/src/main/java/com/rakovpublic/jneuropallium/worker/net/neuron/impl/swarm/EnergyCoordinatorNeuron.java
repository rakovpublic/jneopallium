/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 7 energy coordinator. Defers a task to a higher-battery peer
 * when own battery is below a threshold and at least one peer is
 * meaningfully better off. Loop=2 / Epoch=1.
 */
public class EnergyCoordinatorNeuron extends ModulatableNeuron implements IEnergyCoordinatorNeuron {

    private final Map<String, Double> peerBatteries = new HashMap<>();
    private double ownBattery = 1.0;
    private double deferThreshold = 0.30;
    private double advantageThreshold = 0.20;

    public EnergyCoordinatorNeuron() { super(); }
    public EnergyCoordinatorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setOwnBattery(double f) { this.ownBattery = Math.max(0.0, Math.min(1.0, f)); }
    @Override public double getOwnBattery() { return ownBattery; }

    public void setDeferThreshold(double t) { this.deferThreshold = Math.max(0.0, Math.min(1.0, t)); }
    public void setAdvantageThreshold(double t) { this.advantageThreshold = Math.max(0.0, Math.min(1.0, t)); }

    @Override
    public void onPeerState(PeerStateSignal s) {
        if (s == null || s.getPeerId() == null) return;
        peerBatteries.put(s.getPeerId(), s.getBattery());
    }

    @Override
    public String shouldDeferTo(String taskId) {
        if (taskId == null || ownBattery > deferThreshold) return null;
        String best = null;
        double bestBattery = ownBattery + advantageThreshold;
        for (Map.Entry<String, Double> e : peerBatteries.entrySet()) {
            if (e.getValue() > bestBattery) { best = e.getKey(); bestBattery = e.getValue(); }
        }
        return best;
    }
}
