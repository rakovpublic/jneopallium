/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.HintSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 4 graduated hint generator. For each {@code itemId} it tracks how
 * many hints have been issued and escalates
 * META_COGNITIVE → CONCEPTUAL → WORKED_EXAMPLE.
 * Loop=1 / Epoch=2.
 */
public class HintGenerationNeuron extends ModulatableNeuron {

    private final Map<String, Integer> itemHintCount = new HashMap<>();
    private int maxLevels = 3;

    public HintGenerationNeuron() { super(); }
    public HintGenerationNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    /**
     * Request the next hint for an item. Returns null once all levels
     * (or {@code maxLevels}) are exhausted.
     */
    public HintSignal nextHint(String itemId, String conceptId) {
        int n = itemHintCount.getOrDefault(itemId, 0);
        if (n >= Math.min(maxLevels, HintLevel.values().length)) return null;
        HintLevel level = HintLevel.values()[n];
        String text = generateText(level, conceptId);
        itemHintCount.put(itemId, n + 1);
        HintSignal s = new HintSignal(itemId, level, text);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public int hintsIssuedFor(String itemId) {
        return itemHintCount.getOrDefault(itemId, 0);
    }

    public void reset(String itemId) { itemHintCount.remove(itemId); }
    public void setMaxLevels(int n) { this.maxLevels = Math.max(1, n); }
    public int getMaxLevels() { return maxLevels; }

    private String generateText(HintLevel level, String conceptId) {
        switch (level) {
            case META_COGNITIVE:
                return "What strategy have you used for similar " + conceptId + " problems?";
            case CONCEPTUAL:
                return "Recall the core idea of " + conceptId + " before attempting again.";
            case WORKED_EXAMPLE:
                return "Here is a worked example for " + conceptId + ".";
            default:
                return "";
        }
    }
}
