/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;

/**
 * Layer 0 front-end for a neural amplifier (Intan / Blackrock / Neuropixels).
 * Converts raw voltage samples into {@link NeuralSpikeSignal} using a simple
 * threshold-crossing detector. In production this is replaced by a
 * hardware-specific adapter; the signal boundary stays the same.
 * Loop=1 / Epoch=1.
 */
public class SpikeRecordingNeuron extends ModulatableNeuron implements ISpikeRecordingNeuron {

    private double thresholdUV = 80.0;
    private int channelId;

    public SpikeRecordingNeuron() { super(); }
    public SpikeRecordingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setChannelId(int c) { this.channelId = c; }
    public int getChannelId() { return channelId; }
    public void setThresholdUV(double t) { this.thresholdUV = t; }
    public double getThresholdUV() { return thresholdUV; }

    /**
     * Scan a raw voltage trace; emit a spike for each threshold crossing.
     * Returns the first spike detected, or null.
     */
    public NeuralSpikeSignal detect(double[] samples, long startTsNs, long sampleIntervalNs) {
        if (samples == null) return null;
        for (int i = 1; i < samples.length; i++) {
            if (samples[i - 1] < thresholdUV && samples[i] >= thresholdUV) {
                int from = Math.max(0, i - 8);
                int to = Math.min(samples.length, i + 24);
                double[] snippet = new double[to - from];
                System.arraycopy(samples, from, snippet, 0, snippet.length);
                NeuralSpikeSignal s = new NeuralSpikeSignal(channelId, -1, snippet,
                        startTsNs + i * sampleIntervalNs);
                s.setSourceNeuronId(this.getId());
                return s;
            }
        }
        return null;
    }
}
