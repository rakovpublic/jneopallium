/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.InflammationBroadcastSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 2 per-entity baseline. Holds an EWMA of feature vectors and
 * suspends updates when an {@link InflammationBroadcastSignal} raises
 * the alert above WATCH so the baseline isn't polluted during an
 * active attack. Loop=2 / Epoch=3.
 */
public class EntityBehaviourBaselineNeuron extends ModulatableNeuron implements IEntityBehaviourBaselineNeuron {

    private final Map<String, double[]> baselines = new HashMap<>();
    private long windowTicks = 864_000L;
    private boolean frozen;
    private double alpha = 1.0 / 64.0;

    public EntityBehaviourBaselineNeuron() { super(); }
    public EntityBehaviourBaselineNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setAlpha(double a) { this.alpha = Math.max(1e-6, Math.min(1.0, a)); }

    @Override
    public void update(String entityId, double[] featureVector) {
        if (frozen || entityId == null || featureVector == null) return;
        double[] b = baselines.get(entityId);
        if (b == null) {
            baselines.put(entityId, featureVector.clone());
            return;
        }
        int n = Math.min(b.length, featureVector.length);
        for (int i = 0; i < n; i++) b[i] = (1 - alpha) * b[i] + alpha * featureVector[i];
    }

    @Override
    public double[] baselineFor(String entityId) {
        double[] b = baselines.get(entityId);
        return b == null ? null : b.clone();
    }

    @Override
    public void onInflammation(InflammationBroadcastSignal s) {
        if (s == null) { frozen = false; return; }
        AlertLevel l = s.getLevel();
        frozen = !(l == null || l == AlertLevel.CLEAR || l == AlertLevel.WATCH);
    }

    @Override public boolean isFrozen() { return frozen; }
    @Override public void setWindowTicks(long t) { this.windowTicks = Math.max(1L, t); }
    @Override public long getWindowTicks() { return windowTicks; }
}
