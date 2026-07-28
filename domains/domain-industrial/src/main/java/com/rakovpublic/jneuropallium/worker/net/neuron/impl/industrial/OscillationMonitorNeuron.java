/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Layer 7 oscillation monitor. Uses the autocorrelation at lag=2 as a
 * cheap proxy for periodicity (autocorrelation close to -1 indicates a
 * tight oscillation). Maps the severity to the graduated interventions
 * from spec §6. Loop=1 / Epoch=2.
 */
public class OscillationMonitorNeuron extends ModulatableNeuron implements IOscillationMonitorNeuron {

    private final Map<String, Deque<Double>> windows = new HashMap<>();
    private int acfWindowTicks = 200;

    public OscillationMonitorNeuron() { super(); }
    public OscillationMonitorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setAcfWindowTicks(int w) { this.acfWindowTicks = Math.max(8, w); }
    @Override public int getAcfWindowTicks() { return acfWindowTicks; }

    @Override
    public void observe(MeasurementSignal m) {
        if (m == null || m.getTag() == null) return;
        Deque<Double> w = windows.computeIfAbsent(m.getTag(), k -> new ArrayDeque<>());
        w.addLast(m.getMeasurement());
        while (w.size() > acfWindowTicks) w.removeFirst();
    }

    @Override
    public double severity(String tag) {
        Deque<Double> w = windows.get(tag);
        if (w == null || w.size() < 4) return 0.0;
        double[] arr = new double[w.size()];
        int i = 0;
        double mean = 0.0;
        for (Double v : w) { arr[i] = v; mean += v; i++; }
        mean /= arr.length;
        double num = 0.0, den = 0.0;
        // Control-loop oscillation shows as negative ACF at lag 1.
        for (int j = 1; j < arr.length; j++) num += (arr[j] - mean) * (arr[j - 1] - mean);
        for (double d : arr) den += (d - mean) * (d - mean);
        if (den <= 0) return 0.0;
        double rho = num / den;
        // Tight alternation ⇒ rho near -1; no oscillation ⇒ rho near 0 or positive.
        return Math.max(0.0, Math.min(1.0, -rho));
    }

    @Override
    public OscillationIntervention intervention(String tag) {
        double s = severity(tag);
        if (s < 0.30) return OscillationIntervention.NONE;
        if (s < 0.60) return OscillationIntervention.SCALE_WEIGHTS;
        if (s < 0.85) return OscillationIntervention.INJECT_INHIBITION;
        // Per spec §6: ≥0.85 is BREAK_CONNECTION; sustained-and-extreme
        // (rho saturated at -1) escalates to QUARANTINE_NEURON.
        if (s < 0.98) return OscillationIntervention.BREAK_CONNECTION;
        return OscillationIntervention.QUARANTINE_NEURON;
    }
}
