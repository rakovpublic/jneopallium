package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
