package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.ThreatHypothesisSignal;

import java.util.List;

public interface IThreatHypothesisNeuron extends IModulatableNeuron {
    void seed(String hypothesisId, ThreatCategory category);
    ThreatHypothesisSignal updateFromSignature(SignatureMatchSignal m, String hypothesisId);
    ThreatHypothesisSignal updateFromAnomaly(AnomalyScoreSignal a, String hypothesisId);
    List<ThreatHypothesisSignal> ranked();
    void setPosteriorThreshold(double t);
    double getPosteriorThreshold();
    Double posteriorOf(String hypothesisId);
    int size();
}
