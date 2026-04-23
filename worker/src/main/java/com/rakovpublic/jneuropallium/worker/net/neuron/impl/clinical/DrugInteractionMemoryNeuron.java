/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Layer 3 drug-drug interaction table. Stores symmetric pairs of RxNorm
 * codes with a severity rating (1=minor, 2=moderate, 3=major, 4=contra).
 * Emits a hazard record whenever a proposed drug intersects with a
 * currently-administered one. Biological analogue: inhibitory
 * interneuron that short-circuits dangerous action patterns.
 * Loop=2 / Epoch=1.
 */
public class DrugInteractionMemoryNeuron extends ModulatableNeuron implements IDrugInteractionMemoryNeuron {

    /** Immutable DDI record. */
    public static final class Interaction {
        private final String rxA;
        private final String rxB;
        private final int severity;   // 1..4
        private final String mechanism;
        private final String citation;

        public Interaction(String rxA, String rxB, int severity, String mechanism, String citation) {
            this.rxA = rxA;
            this.rxB = rxB;
            this.severity = Math.max(1, Math.min(4, severity));
            this.mechanism = mechanism;
            this.citation = citation;
        }

        public String getRxA() { return rxA; }
        public String getRxB() { return rxB; }
        public int getSeverity() { return severity; }
        public String getMechanism() { return mechanism; }
        public String getCitation() { return citation; }
        public boolean isContraindication() { return severity >= 4; }
    }

    private final Map<String, Map<String, Interaction>> table = new HashMap<>();
    private final Set<String> currentRegimen = new HashSet<>();

    public DrugInteractionMemoryNeuron() { super(); }

    public DrugInteractionMemoryNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void addInteraction(Interaction it) {
        if (it == null || it.getRxA() == null || it.getRxB() == null) return;
        table.computeIfAbsent(it.getRxA(), k -> new HashMap<>()).put(it.getRxB(), it);
        table.computeIfAbsent(it.getRxB(), k -> new HashMap<>()).put(it.getRxA(), it);
    }

    public void addActive(String rxNorm) { if (rxNorm != null) currentRegimen.add(rxNorm); }
    public void removeActive(String rxNorm) { if (rxNorm != null) currentRegimen.remove(rxNorm); }
    public Set<String> getActive() { return Collections.unmodifiableSet(currentRegimen); }

    /** Returns all interactions between {@code proposed} and any drug in the regimen. */
    public List<Interaction> hazardsFor(String proposed) {
        List<Interaction> out = new ArrayList<>();
        if (proposed == null) return out;
        Map<String, Interaction> row = table.get(proposed);
        if (row == null) return out;
        for (String active : currentRegimen) {
            Interaction it = row.get(active);
            if (it != null) out.add(it);
        }
        return out;
    }

    public int maxSeverityFor(String proposed) {
        int max = 0;
        for (Interaction it : hazardsFor(proposed)) {
            if (it.getSeverity() > max) max = it.getSeverity();
        }
        return max;
    }

    public boolean isContraindicatedWithRegimen(String proposed) {
        return maxSeverityFor(proposed) >= 4;
    }

    public int interactionCount() {
        int n = 0;
        for (Map<String, Interaction> row : table.values()) n += row.size();
        return n / 2;
    }

    public void clear() { table.clear(); currentRegimen.clear(); }
}
