package com.rakovpublic.jneuropallium.ai.neurons.action;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface IActionSelectionNeuron extends IModulatableNeuron {
    boolean hasPendingVeto(String actionPlanId);
    void addVeto(String actionPlanId);
    Map<String, MotorCommandSignal> getCandidates();
    void setCandidates(Map<String, MotorCommandSignal> candidates);
    Set<String> getPendingVetoes();
    void setPendingVetoes(Set<String> pendingVetoes);
    double getConfidenceThreshold();
    void setConfidenceThreshold(double confidenceThreshold);
}
