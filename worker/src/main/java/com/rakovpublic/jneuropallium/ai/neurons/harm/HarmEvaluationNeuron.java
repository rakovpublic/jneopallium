package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.HarmThreshold;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Harm evaluation neuron used by SimulationAggregationProcessor.
 * Aggregates multi-step consequence simulation results and classifies the overall harm verdict
 * using per-dimension thresholds.
 */
public class HarmEvaluationNeuron extends ModulatableNeuron {

    private HarmThreshold threshold;
    private Map<String, HarmThreshold> dimensionThresholds;

    public HarmEvaluationNeuron() {
        super();
        this.threshold = new HarmThreshold();
        this.dimensionThresholds = new HashMap<>();
        initDefaultDimensionThresholds();
    }

    public HarmEvaluationNeuron(Long neuronId,
                                com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                Long run,
                                HarmThreshold threshold) {
        super(neuronId, chain, run);
        this.threshold = threshold;
        this.dimensionThresholds = new HashMap<>();
        initDefaultDimensionThresholds();
    }

    private void initDefaultDimensionThresholds() {
        dimensionThresholds.put("physicalIntegrity", new HarmThreshold(0.1, 0.01, 0.3));
        dimensionThresholds.put("autonomy",          new HarmThreshold(0.3, 0.05, 0.5));
        dimensionThresholds.put("resource",          new HarmThreshold(0.4, 0.1,  0.6));
        dimensionThresholds.put("information",       new HarmThreshold(0.3, 0.05, 0.5));
        dimensionThresholds.put("emotional",         new HarmThreshold(0.35, 0.07, 0.55));
    }

    public HarmThreshold getThreshold() { return threshold; }
    public void setThreshold(HarmThreshold threshold) { this.threshold = threshold; }

    public Map<String, HarmThreshold> getDimensionThresholds() { return dimensionThresholds; }
    public void setDimensionThresholds(Map<String, HarmThreshold> dimensionThresholds) { this.dimensionThresholds = dimensionThresholds; }
}
