/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;

/**
 * Layer 3 Bayesian Knowledge Tracing (Corbett &amp; Anderson 1995) per
 * concept. Tracks P(knows) with slip / guess parameters; emits
 * {@link MasteryUpdateSignal} whenever the estimate moves appreciably.
 * Loop=2 / Epoch=3.
 */
public class ConceptMasteryNeuron extends ModulatableNeuron {

    private String conceptId;
    private double pKnow = 0.1;     // prior P(knows)
    private double pLearn = 0.15;   // P(learn on a step)
    private double pSlip = 0.1;     // P(slip) — knows but wrong
    private double pGuess = 0.2;    // P(guess) — doesn't know but right
    private double lastEmitted = -1.0;
    private double emitDelta = 0.02;

    public ConceptMasteryNeuron() { super(); }
    public ConceptMasteryNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public ConceptMasteryNeuron(String conceptId) {
        super();
        this.conceptId = conceptId;
    }

    /**
     * Update posterior P(knows) given an observed correctness.
     * Returns a MasteryUpdateSignal when the change exceeds emitDelta, else null.
     */
    public MasteryUpdateSignal update(boolean correct) {
        double num = correct
                ? pKnow * (1 - pSlip)
                : pKnow * pSlip;
        double den = correct
                ? pKnow * (1 - pSlip) + (1 - pKnow) * pGuess
                : pKnow * pSlip + (1 - pKnow) * (1 - pGuess);
        double posteriorGivenObs = den <= 0 ? pKnow : num / den;
        pKnow = posteriorGivenObs + (1 - posteriorGivenObs) * pLearn;
        pKnow = clamp01(pKnow);

        if (lastEmitted < 0 || Math.abs(pKnow - lastEmitted) >= emitDelta) {
            lastEmitted = pKnow;
            MasteryUpdateSignal s = new MasteryUpdateSignal(conceptId, pKnow);
            s.setSourceNeuronId(this.getId());
            return s;
        }
        return null;
    }

    public String getConceptId() { return conceptId; }
    public void setConceptId(String c) { this.conceptId = c; }
    public double getPKnow() { return pKnow; }
    public void setPKnow(double v) { this.pKnow = clamp01(v); }
    public double getPLearn() { return pLearn; }
    public void setPLearn(double v) { this.pLearn = clamp01(v); }
    public double getPSlip() { return pSlip; }
    public void setPSlip(double v) { this.pSlip = clamp01(v); }
    public double getPGuess() { return pGuess; }
    public void setPGuess(double v) { this.pGuess = clamp01(v); }
    public void setEmitDelta(double d) { this.emitDelta = Math.max(0.0, d); }

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
