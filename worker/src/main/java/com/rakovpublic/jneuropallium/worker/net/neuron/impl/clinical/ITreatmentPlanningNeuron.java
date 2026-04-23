package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal;

import java.util.List;

public interface ITreatmentPlanningNeuron extends IModulatableNeuron {
    void setWeightKg(double kg);
    double getWeightKg();
    void setVulnerabilityFactor(double f);
    double getVulnerabilityFactor();
    void setPatientId(String p);
    String getPatientId();
    List<TreatmentProposalSignal> plan(List<TreatmentPlanningNeuron.Candidate> candidates);
}
