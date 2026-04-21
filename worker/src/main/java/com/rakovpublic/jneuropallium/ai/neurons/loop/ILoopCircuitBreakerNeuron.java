package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.model.ActiveIntervention;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import java.util.HashMap;
import java.util.Map;

public interface ILoopCircuitBreakerNeuron extends IModulatableNeuron {
    Map<String, Integer> getInterventionHistory();
    void setInterventionHistory(Map<String, Integer> interventionHistory);
    Map<String, ActiveIntervention> getActiveInterventions();
    void setActiveInterventions(Map<String, ActiveIntervention> activeInterventions);
    int getMaxInterventions();
    void setMaxInterventions(int maxInterventions);
}
