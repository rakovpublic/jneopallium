/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Snapshot of the agent's current body schema: known effectors and their
 * capabilities.
 * Biological analogue: posterior parietal body schema, updated by
 * proprioceptive feedback and tool use (Head &amp; Holmes 1911, Maravita 2004).
 */
public final class BodySchema implements Serializable {

    private final Map<Integer, EffectorCapability> effectors;

    public BodySchema() {
        this.effectors = new HashMap<>();
    }

    public BodySchema(Map<Integer, EffectorCapability> effectors) {
        this.effectors = effectors == null ? new HashMap<>() : new HashMap<>(effectors);
    }

    public Map<Integer, EffectorCapability> getEffectors() {
        return Collections.unmodifiableMap(effectors);
    }

    public EffectorCapability get(int effectorId) {
        return effectors.get(effectorId);
    }

    public boolean has(int effectorId) {
        return effectors.containsKey(effectorId);
    }

    public int size() { return effectors.size(); }

    public BodySchema with(int effectorId, EffectorCapability cap) {
        Map<Integer, EffectorCapability> m = new HashMap<>(effectors);
        m.put(effectorId, cap);
        return new BodySchema(m);
    }

    public BodySchema without(int effectorId) {
        Map<Integer, EffectorCapability> m = new HashMap<>(effectors);
        m.remove(effectorId);
        return new BodySchema(m);
    }
}
