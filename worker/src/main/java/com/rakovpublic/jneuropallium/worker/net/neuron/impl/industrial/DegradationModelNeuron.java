/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 3 per-asset RUL estimator. Each wear measurement consumes a
 * small fraction of the remaining-useful-life hours. Simple but
 * honest: deployments wire in a proper fatigue / Arrhenius model
 * behind the same interface. Loop=2 / Epoch=3.
 */
public class DegradationModelNeuron extends ModulatableNeuron implements IDegradationModelNeuron {

    private static final class Asset { double rulHours; double confidence = 0.5; int samples; }
    private final Map<String, Asset> assets = new HashMap<>();
    private double wearPerUnit = 1.0;

    public DegradationModelNeuron() { super(); }
    public DegradationModelNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setWearPerUnit(double w) { this.wearPerUnit = Math.max(0.0, w); }

    @Override public void seedAsset(String assetId, double initialRulHours) {
        if (assetId == null) return;
        Asset a = new Asset();
        a.rulHours = Math.max(0.0, initialRulHours);
        assets.put(assetId, a);
    }

    @Override
    public DegradationSignal observe(String assetId, MeasurementSignal wear) {
        if (assetId == null || wear == null) return null;
        Asset a = assets.get(assetId);
        if (a == null) { seedAsset(assetId, 1000.0); a = assets.get(assetId); }
        double consume = Math.max(0.0, wear.getMeasurement()) * wearPerUnit;
        a.rulHours = Math.max(0.0, a.rulHours - consume);
        a.samples++;
        a.confidence = Math.min(1.0, 0.5 + a.samples / 200.0);
        return new DegradationSignal(assetId, a.rulHours, a.confidence);
    }

    @Override public double rulFor(String assetId) {
        Asset a = assets.get(assetId);
        return a == null ? Double.NaN : a.rulHours;
    }

    @Override public int trackedAssets() { return assets.size(); }
}
