package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Harm gate neuron used by HarmInterceptionProcessor and HarmResultProcessor.
 * Acts as a safety checkpoint: caches verdicts, queues pending motor commands,
 * and applies the precautionary principle for uncertain assessments.
 */
public class HarmGateNeuron extends ModulatableNeuron {

    private Map<String, HarmAssessmentSignal> assessmentCache;
    private Queue<MotorCommandSignal> pendingQueue;
    private Map<String, Integer> uncertaintyRetryCount;
    private int simulationDepth;
    private double uncertaintyThreshold;

    public HarmGateNeuron() {
        super();
        this.assessmentCache = new HashMap<>();
        this.pendingQueue = new LinkedList<>();
        this.uncertaintyRetryCount = new HashMap<>();
        this.simulationDepth = 3;
        this.uncertaintyThreshold = 0.6;
    }

    public HarmGateNeuron(Long neuronId,
                          com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                          Long run,
                          int simulationDepth,
                          double uncertaintyThreshold) {
        super(neuronId, chain, run);
        this.assessmentCache = new HashMap<>();
        this.pendingQueue = new LinkedList<>();
        this.uncertaintyRetryCount = new HashMap<>();
        this.simulationDepth = simulationDepth;
        this.uncertaintyThreshold = uncertaintyThreshold;
    }

    public HarmAssessmentSignal getCachedAssessment(String planId) {
        return assessmentCache.get(planId);
    }

    public void cacheAssessment(String planId, HarmAssessmentSignal assessment) {
        assessmentCache.put(planId, assessment);
    }

    public Map<String, HarmAssessmentSignal> getAssessmentCache() { return assessmentCache; }
    public void setAssessmentCache(Map<String, HarmAssessmentSignal> assessmentCache) { this.assessmentCache = assessmentCache; }

    public Queue<MotorCommandSignal> getPendingQueue() { return pendingQueue; }
    public void setPendingQueue(Queue<MotorCommandSignal> pendingQueue) { this.pendingQueue = pendingQueue; }

    public Map<String, Integer> getUncertaintyRetryCount() { return uncertaintyRetryCount; }
    public void setUncertaintyRetryCount(Map<String, Integer> uncertaintyRetryCount) { this.uncertaintyRetryCount = uncertaintyRetryCount; }

    public int getSimulationDepth() { return simulationDepth; }
    public void setSimulationDepth(int simulationDepth) { this.simulationDepth = simulationDepth; }

    public double getUncertaintyThreshold() { return uncertaintyThreshold; }
    public void setUncertaintyThreshold(double uncertaintyThreshold) { this.uncertaintyThreshold = uncertaintyThreshold; }
}
