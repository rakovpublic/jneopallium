/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 3 clinical-practice-guideline memory. Specialises the
 * autonomous-AI {@code LongTermMemoryNeuron} pattern: stores CPG
 * templates keyed by ICD-10 so that planners and recommender neurons can
 * retrieve and cite them. Every lookup returns both the recommendation
 * payload and the required citation string. Loop=2 / Epoch=1.
 */
public class GuidelineMemoryNeuron extends ModulatableNeuron implements IGuidelineMemoryNeuron {

    /** Immutable guideline record. */
    public static final class Guideline {
        private final String icd10;
        private final String recommendation;
        private final String citation;
        private final List<String> firstLineCodes;
        private final List<String> contraindicationCodes;
        private final long version;

        public Guideline(String icd10, String recommendation, String citation,
                         List<String> firstLineCodes, List<String> contraindicationCodes, long version) {
            this.icd10 = icd10;
            this.recommendation = recommendation;
            this.citation = citation;
            this.firstLineCodes = firstLineCodes == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(firstLineCodes));
            this.contraindicationCodes = contraindicationCodes == null ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(contraindicationCodes));
            this.version = version;
        }

        public String getIcd10() { return icd10; }
        public String getRecommendation() { return recommendation; }
        public String getCitation() { return citation; }
        public List<String> getFirstLineCodes() { return firstLineCodes; }
        public List<String> getContraindicationCodes() { return contraindicationCodes; }
        public long getVersion() { return version; }
    }

    private final Map<String, Guideline> byIcd10 = new HashMap<>();

    public GuidelineMemoryNeuron() { super(); }

    public GuidelineMemoryNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void store(Guideline g) {
        if (g == null || g.getIcd10() == null) return;
        byIcd10.put(g.getIcd10(), g);
    }

    public Guideline lookup(String icd10) {
        return icd10 == null ? null : byIcd10.get(icd10);
    }

    public boolean isFirstLine(String icd10, String rxNormOrProcedureCode) {
        Guideline g = lookup(icd10);
        if (g == null || rxNormOrProcedureCode == null) return false;
        return g.getFirstLineCodes().contains(rxNormOrProcedureCode);
    }

    public boolean isContraindicatedByGuideline(String icd10, String code) {
        Guideline g = lookup(icd10);
        if (g == null || code == null) return false;
        return g.getContraindicationCodes().contains(code);
    }

    public String citeFor(String icd10) {
        Guideline g = lookup(icd10);
        return g == null ? null : g.getCitation();
    }

    public int size() { return byIcd10.size(); }
    public void clear() { byIcd10.clear(); }
}
