/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 3 personal motor lexicon. Stores user-defined gestures as named
 * latent templates; allows the user to invent new control primitives and
 * associate them with action macros (e.g. "compose message", "scroll page").
 * Biological analogue: idiosyncratic motor-schema learning in M1 / premotor.
 * Loop=2 / Epoch=1.
 */
public class PersonalMotorLexiconNeuron extends ModulatableNeuron implements IPersonalMotorLexiconNeuron {

    private final Map<String, double[]> lexicon = new HashMap<>();
    private double matchThreshold = 0.7;

    public PersonalMotorLexiconNeuron() { super(); }
    public PersonalMotorLexiconNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void register(String name, double[] template) {
        if (name == null || template == null) return;
        lexicon.put(name, template.clone());
    }

    public boolean forget(String name) { return lexicon.remove(name) != null; }
    public int size() { return lexicon.size(); }

    /**
     * Find the closest lexicon entry to {@code query} by cosine similarity;
     * return its name or {@code null} if below {@link #matchThreshold}.
     */
    public String match(double[] query) {
        if (query == null || lexicon.isEmpty()) return null;
        String best = null;
        double bestSim = -1;
        for (Map.Entry<String, double[]> e : lexicon.entrySet()) {
            double sim = cosine(query, e.getValue());
            if (sim > bestSim) { bestSim = sim; best = e.getKey(); }
        }
        return bestSim >= matchThreshold ? best : null;
    }

    public void setMatchThreshold(double t) { this.matchThreshold = Math.max(0, Math.min(1, t)); }

    private static double cosine(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < n; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
