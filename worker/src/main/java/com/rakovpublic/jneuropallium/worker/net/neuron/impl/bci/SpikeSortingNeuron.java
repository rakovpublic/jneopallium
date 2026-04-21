/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;

import java.util.ArrayList;
import java.util.List;

/**
 * Layer 0 online spike sorter. Maintains a small bank of per-unit waveform
 * templates and assigns each incoming spike to the nearest template (or
 * creates a new one).
 * Loop=1 / Epoch=1.
 */
public class SpikeSortingNeuron extends ModulatableNeuron implements ISpikeSortingNeuron {

    private final List<double[]> templates = new ArrayList<>();
    private double matchThreshold = 0.8;
    private int maxUnits = 8;

    public SpikeSortingNeuron() { super(); }
    public SpikeSortingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Assign a unit id to the spike and update the matching template.
     * Returns the unit id assigned.
     */
    public int sort(NeuralSpikeSignal spike) {
        double[] w = spike.getWaveformSnippet();
        if (w == null || w.length == 0) { spike.setUnitId(-1); return -1; }
        int best = -1;
        double bestSim = -1.0;
        for (int i = 0; i < templates.size(); i++) {
            double sim = cosine(w, templates.get(i));
            if (sim > bestSim) { bestSim = sim; best = i; }
        }
        if (bestSim >= matchThreshold) {
            average(templates.get(best), w, 0.1);
            spike.setUnitId(best);
            return best;
        }
        if (templates.size() < maxUnits) {
            templates.add(w.clone());
            int id = templates.size() - 1;
            spike.setUnitId(id);
            return id;
        }
        spike.setUnitId(best);
        return best;
    }

    public int unitCount() { return templates.size(); }
    public void setMatchThreshold(double t) { this.matchThreshold = t; }
    public void setMaxUnits(int n) { this.maxUnits = n; }

    private static double cosine(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        if (na == 0 || nb == 0) return 0;
        return dot / Math.sqrt(na * nb);
    }

    private static void average(double[] template, double[] w, double alpha) {
        int n = Math.min(template.length, w.length);
        for (int i = 0; i < n; i++) template[i] = (1 - alpha) * template[i] + alpha * w[i];
    }
}
