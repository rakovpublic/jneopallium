package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.List;
import java.util.Set;

public interface IDrugInteractionMemoryNeuron extends IModulatableNeuron {
    void addInteraction(DrugInteractionMemoryNeuron.Interaction it);
    void addActive(String rxNorm);
    void removeActive(String rxNorm);
    Set<String> getActive();
    List<DrugInteractionMemoryNeuron.Interaction> hazardsFor(String proposed);
    int maxSeverityFor(String proposed);
    boolean isContraindicatedWithRegimen(String proposed);
    int interactionCount();
    void clear();
}
