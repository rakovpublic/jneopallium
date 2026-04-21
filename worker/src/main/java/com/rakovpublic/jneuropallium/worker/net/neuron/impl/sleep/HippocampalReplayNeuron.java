/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.ReplaySignal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selects high-salience recent episodes and emits compressed replays in
 * the configured direction. Maintains a bounded recent-episode buffer.
 * Layer 3, loop=2 / epoch=3.
 * <p>Biological analogue: CA1 place-cell sequence reactivation during
 * rest (Wilson &amp; McNaughton 1994; Foster &amp; Wilson 2006).
 */
public class HippocampalReplayNeuron extends ModulatableNeuron implements IHippocampalReplayNeuron {

    private final LinkedHashMap<String, Episode> buffer = new LinkedHashMap<>();
    private int topK = 20;
    private double compressionRatio = 10.0;
    private ReplayDirection direction = ReplayDirection.REVERSE;
    private final Random rng = new Random(0xC0FFEEL);

    public HippocampalReplayNeuron() { super(); }

    public HippocampalReplayNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void recordEpisode(String id, List<Long> neuronSequence, double salience) {
        if (id == null || neuronSequence == null) return;
        buffer.put(id, new Episode(id, new ArrayList<>(neuronSequence), Math.max(0.0, salience)));
    }

    /**
     * Choose the top-K by salience and emit replays, each in the
     * configured direction, at the configured compression ratio.
     */
    public List<ReplaySignal> emitTopK() {
        List<Episode> eps = new ArrayList<>(buffer.values());
        eps.sort((a, b) -> Double.compare(b.salience, a.salience));
        List<ReplaySignal> out = new ArrayList<>();
        int n = Math.min(topK, eps.size());
        for (int i = 0; i < n; i++) {
            Episode e = eps.get(i);
            List<Long> seq = new ArrayList<>(e.sequence);
            switch (direction) {
                case REVERSE: Collections.reverse(seq); break;
                case SHUFFLED: Collections.shuffle(seq, rng); break;
                case FORWARD: default: break;
            }
            ReplaySignal r = new ReplaySignal(e.id, direction, compressionRatio, seq);
            r.setSourceNeuronId(this.getId());
            out.add(r);
        }
        return out;
    }

    public int bufferedEpisodes() { return buffer.size(); }
    public int getTopK() { return topK; }
    public void setTopK(int v) { this.topK = Math.max(0, v); }
    public double getCompressionRatio() { return compressionRatio; }
    public void setCompressionRatio(double v) { this.compressionRatio = Math.max(1.0, v); }
    public ReplayDirection getDirection() { return direction; }
    public void setDirection(ReplayDirection d) { this.direction = d == null ? ReplayDirection.REVERSE : d; }

    private static class Episode {
        final String id;
        final List<Long> sequence;
        final double salience;
        Episode(String id, List<Long> sequence, double salience) {
            this.id = id; this.sequence = sequence; this.salience = salience;
        }
    }

    public Map<String, Double> debugSalienceMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Map.Entry<String, Episode> e : buffer.entrySet()) m.put(e.getKey(), e.getValue().salience);
        return m;
    }
}
