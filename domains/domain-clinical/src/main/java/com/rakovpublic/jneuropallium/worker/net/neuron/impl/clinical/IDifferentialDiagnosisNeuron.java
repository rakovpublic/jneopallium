package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal;

import java.util.List;

public interface IDifferentialDiagnosisNeuron extends IModulatableNeuron {
    void setMaxCandidates(int n);
    int getMaxCandidates();
    void setPosteriorThreshold(double t);
    double getPosteriorThreshold();
    void setPatientId(String p);
    String getPatientId();
    void seed(String icd10);
    void update(String icd10, double likelihoodRatio, String evidenceId);
    List<DiagnosisHypothesisSignal> ranked();
    Double posteriorOf(String icd10);
    int size();
    void reset();
    boolean hasConfidentWinner(double margin);
}
