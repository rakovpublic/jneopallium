/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.HashSet;
import java.util.Set;

/**
 * Layer 0 artefact mask. Detects movement and stim-artefact excursions and
 * blacklists affected channels for a configurable window.
 * Loop=1 / Epoch=1.
 */
public class ArtefactRejectionNeuron extends ModulatableNeuron implements IArtefactRejectionNeuron {

    private double absAmplitudeLimitUV = 500.0;
    private final Set<Integer> maskedChannels = new HashSet<>();

    public ArtefactRejectionNeuron() { super(); }
    public ArtefactRejectionNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Check a window for excursions; mask the channel if any sample exceeds
     * the limit. Returns true if the channel is now masked.
     */
    public boolean check(int channelId, double[] window) {
        if (window == null) return maskedChannels.contains(channelId);
        for (double v : window) {
            if (Math.abs(v) >= absAmplitudeLimitUV) {
                maskedChannels.add(channelId);
                return true;
            }
        }
        return maskedChannels.contains(channelId);
    }

    public boolean isMasked(int channelId) { return maskedChannels.contains(channelId); }
    public void unmask(int channelId) { maskedChannels.remove(channelId); }
    public void setAbsAmplitudeLimitUV(double v) { this.absAmplitudeLimitUV = v; }
    public int maskedCount() { return maskedChannels.size(); }
}
