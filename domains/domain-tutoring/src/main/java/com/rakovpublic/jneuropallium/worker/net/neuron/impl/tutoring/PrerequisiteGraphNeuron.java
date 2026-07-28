/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.*;

/**
 * Layer 3 curriculum prerequisite DAG. Holds per-concept dependencies and
 * exposes an "eligible next concepts" query constrained by mastery of
 * prerequisites. Consulted by {@link ZPDPlanningNeuron}.
 * Loop=2 / Epoch=5.
 */
public class PrerequisiteGraphNeuron extends ModulatableNeuron implements IPrerequisiteGraphNeuron {

    private final Map<String, Set<String>> prerequisites = new HashMap<>();
    private double masteryThreshold = 0.7;

    public PrerequisiteGraphNeuron() { super(); }
    public PrerequisiteGraphNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void addConcept(String conceptId) {
        prerequisites.computeIfAbsent(conceptId, k -> new HashSet<>());
    }

    public void addPrerequisite(String concept, String prereq) {
        prerequisites.computeIfAbsent(concept, k -> new HashSet<>()).add(prereq);
        prerequisites.computeIfAbsent(prereq, k -> new HashSet<>());
    }

    public Set<String> getPrerequisites(String concept) {
        return Collections.unmodifiableSet(prerequisites.getOrDefault(concept, new HashSet<>()));
    }

    /**
     * Concepts whose prerequisites are all mastered at {@code masteryThreshold}
     * but the concept itself is not yet mastered.
     */
    public List<String> eligibleNext(Map<String, Double> currentMastery) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : prerequisites.entrySet()) {
            String c = e.getKey();
            double m = currentMastery.getOrDefault(c, 0.0);
            if (m >= masteryThreshold) continue;
            boolean ready = true;
            for (String p : e.getValue()) {
                if (currentMastery.getOrDefault(p, 0.0) < masteryThreshold) { ready = false; break; }
            }
            if (ready) out.add(c);
        }
        Collections.sort(out);
        return out;
    }

    public void setMasteryThreshold(double t) { this.masteryThreshold = Math.max(0.0, Math.min(1.0, t)); }
    public double getMasteryThreshold() { return masteryThreshold; }
    public int size() { return prerequisites.size(); }
}
