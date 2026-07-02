/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * The terminal, read-only stage. It turns a maintenance hypothesis into a human
 * advisory when the accumulated evidence clears the (live, feedback-adapted)
 * threshold for its fault family, subject to a per-asset de-duplication window.
 * It holds its thresholds in a live map that {@link #onThresholdUpdate} mutates
 * in place, so the feedback the operators give reshapes gate behaviour while the
 * process keeps running — no restart, no redeploy. It never actuates: the only
 * thing it can emit is a {@link MaintenanceAdvisorySignal}.
 */
public class SsAdvisoryGateNeuron extends ModulatableNeuron implements ISsAdvisoryGateNeuron {

    private final Map<String, Double> thresholds = new HashMap<>();
    private final Map<String, Long> lastFired = new HashMap<>();

    private double defaultThreshold = 1.0;
    private long deduplicationTicks = 60L;

    public SsAdvisoryGateNeuron() { super(); }
    public SsAdvisoryGateNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setThreshold(String family, double threshold) { thresholds.put(family, threshold); }
    public void setDefaultThreshold(double t) { this.defaultThreshold = t; }

    // bean accessors so the deployable layer configuration can bind the initial
    // per-family thresholds as a map (they then adapt live from feedback).
    public Map<String, Double> getThresholds() { return thresholds; }
    public void setThresholds(Map<String, Double> m) {
        thresholds.clear();
        if (m != null) thresholds.putAll(m);
    }
    public double getDeduplicationTicks() { return deduplicationTicks; }
    public void setDeduplicationTicks(long deduplicationTicks) { this.deduplicationTicks = deduplicationTicks; }

    public double thresholdFor(String family) {
        return thresholds.getOrDefault(family, defaultThreshold);
    }

    @Override
    public void onThresholdUpdate(ThresholdUpdateSignal update) {
        if (update == null || update.getFaultFamily() == null) return;
        thresholds.put(update.getFaultFamily(), update.getThreshold());
    }

    @Override
    public MaintenanceAdvisorySignal gate(HealthHypothesisSignal h) {
        if (h == null) return null;
        double threshold = thresholds.getOrDefault(h.getFaultFamily(), defaultThreshold);
        if (h.getEvidence() < threshold) return null;

        Long last = lastFired.get(h.getAssetId());
        if (last != null && h.getTimestamp() - last < deduplicationTicks) return null;
        lastFired.put(h.getAssetId(), h.getTimestamp());

        String recommendation = String.format(
                "Inspect %s for %s within ~%d ticks (severity %.2f, uncertainty %.2f). Advisory only.",
                h.getAssetId(), h.getFaultFamily(), h.getLeadTimeTicks(), h.getSeverity(), h.getUncertainty());
        return new MaintenanceAdvisorySignal(h.getAssetId(), h.getFaultFamily(), h.getSeverity(),
                h.getLeadTimeTicks(), h.getUncertainty(), recommendation, h.getTimestamp());
    }
}
