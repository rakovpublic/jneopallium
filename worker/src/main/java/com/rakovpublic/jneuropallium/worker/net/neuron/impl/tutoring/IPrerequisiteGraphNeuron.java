package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IPrerequisiteGraphNeuron extends IModulatableNeuron {
    void addConcept(String conceptId);
    void addPrerequisite(String concept, String prereq);
    Set<String> getPrerequisites(String concept);
    List<String> eligibleNext(Map<String, Double> currentMastery);
    void setMasteryThreshold(double t);
    double getMasteryThreshold();
    int size();
}
