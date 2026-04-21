package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.Map;
import java.util.Queue;

public interface IHarmGateNeuron extends IModulatableNeuron {
    HarmAssessmentSignal getCachedAssessment(String planId);
    void cacheAssessment(String planId, HarmAssessmentSignal assessment);
    Map<String, HarmAssessmentSignal> getAssessmentCache();
    void setAssessmentCache(Map<String, HarmAssessmentSignal> assessmentCache);
    Queue<MotorCommandSignal> getPendingQueue();
    void setPendingQueue(Queue<MotorCommandSignal> pendingQueue);
    Map<String, Integer> getUncertaintyRetryCount();
    void setUncertaintyRetryCount(Map<String, Integer> uncertaintyRetryCount);
    int getSimulationDepth();
    void setSimulationDepth(int simulationDepth);
    double getUncertaintyThreshold();
    void setUncertaintyThreshold(double uncertaintyThreshold);
}
