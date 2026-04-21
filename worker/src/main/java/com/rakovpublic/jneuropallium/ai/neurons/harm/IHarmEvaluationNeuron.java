package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.HarmThreshold;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ConsequenceSimulationSignal;

import java.util.List;
import java.util.Map;

public interface IHarmEvaluationNeuron extends IModulatableNeuron {
    HarmThreshold getThreshold();
    void setThreshold(HarmThreshold threshold);
    Map<String, HarmThreshold> getDimensionThresholds();
    void setDimensionThresholds(Map<String, HarmThreshold> dimensionThresholds);
    Map<String, List<ConsequenceSimulationSignal>> getPendingSimulations();
    void setPendingSimulations(Map<String, List<ConsequenceSimulationSignal>> pendingSimulations);
}
