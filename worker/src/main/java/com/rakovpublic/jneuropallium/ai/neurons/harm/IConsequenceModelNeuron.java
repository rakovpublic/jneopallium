package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.WorldStateModel;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import java.util.HashMap;
import java.util.Map;

public interface IConsequenceModelNeuron extends IModulatableNeuron {
    double getActionConfidence(String actionType);
    WorldStateModel getWorldModel();
    void setWorldModel(WorldStateModel worldModel);
    Map<String, double[]> getActionEffectWeights();
    void setActionEffectWeights(Map<String, double[]> actionEffectWeights);
    Map<String, Double> getActionConfidences();
    void setActionConfidences(Map<String, Double> actionConfidences);
}
