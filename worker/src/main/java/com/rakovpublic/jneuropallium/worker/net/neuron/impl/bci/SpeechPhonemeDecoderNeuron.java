/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 1 speech-phoneme decoder (Moses et al. 2021; Willett et al. 2023).
 * Maps a feature vector onto a phoneme label via per-class centroid
 * classification. Production deployments swap in an HMM / CTC / RNN
 * acoustic-to-phoneme model.
 * Loop=1 / Epoch=1.
 */
public class SpeechPhonemeDecoderNeuron extends ModulatableNeuron implements ISpeechPhonemeDecoderNeuron {

    private final Map<String, double[]> phonemeCentroids = new HashMap<>();
    private String lastPhoneme = "";
    private double lastConfidence;

    public SpeechPhonemeDecoderNeuron() { super(); }
    public SpeechPhonemeDecoderNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void trainPhoneme(String symbol, double[] centroid) {
        if (symbol == null || centroid == null) return;
        phonemeCentroids.put(symbol, centroid.clone());
    }

    public int vocabSize() { return phonemeCentroids.size(); }

    /**
     * Classify a feature vector; returns the closest phoneme label in Euclidean
     * distance. Confidence is a softmax-like score over the top-two distances.
     */
    public String classify(double[] feature) {
        if (feature == null || phonemeCentroids.isEmpty()) { lastPhoneme = ""; lastConfidence = 0; return ""; }
        String best = "";
        double bestD = Double.POSITIVE_INFINITY;
        double secondD = Double.POSITIVE_INFINITY;
        for (Map.Entry<String, double[]> e : phonemeCentroids.entrySet()) {
            double d = euclid(feature, e.getValue());
            if (d < bestD) { secondD = bestD; bestD = d; best = e.getKey(); }
            else if (d < secondD) secondD = d;
        }
        lastPhoneme = best;
        lastConfidence = secondD == Double.POSITIVE_INFINITY ? 1.0
                : Math.max(0, Math.min(1, (secondD - bestD) / (secondD + bestD + 1e-9)));
        return best;
    }

    public String getLastPhoneme() { return lastPhoneme; }
    public double getLastConfidence() { return lastConfidence; }

    private static double euclid(double[] a, double[] b) {
        int n = Math.min(a.length, b.length);
        double s = 0;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d * d; }
        return Math.sqrt(s);
    }
}
