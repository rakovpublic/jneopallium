package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;

import java.util.List;

public interface IPatientContextNeuron extends IModulatableNeuron {
    void update(DemographicSignal d);
    boolean hasAllergy(String code);
    boolean hasComorbidity(String code);
    String getPatientId();
    int getAgeYears();
    Sex getSex();
    List<String> getComorbidities();
    List<String> getAllergies();
    boolean isPregnant();
    boolean isImmunocompromised();
    double getVulnerabilityFactor();
}
