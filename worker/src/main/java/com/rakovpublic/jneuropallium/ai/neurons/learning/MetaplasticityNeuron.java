package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.HashMap;
import java.util.Map;

/**
 * Metaplasticity neuron used by MetaplasticityProcessor.
 * Adjusts plasticity rates per region to maintain homeostatic activity levels.
 */
public class MetaplasticityNeuron extends ModulatableNeuron {

    private Map<String, Double> activityHistory;
    private Map<String, Double> plasticityRates;
    private double overactiveThreshold;
    private double underactiveThreshold;
    private double plasticityStep;

    public MetaplasticityNeuron() {
        super();
        this.activityHistory = new HashMap<>();
        this.plasticityRates = new HashMap<>();
        this.overactiveThreshold = 80.0;
        this.underactiveThreshold = 5.0;
        this.plasticityStep = 0.005;
    }

    public MetaplasticityNeuron(Long neuronId,
                                com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                Long run,
                                double overactiveThreshold,
                                double underactiveThreshold,
                                double plasticityStep) {
        super(neuronId, chain, run);
        this.activityHistory = new HashMap<>();
        this.plasticityRates = new HashMap<>();
        this.overactiveThreshold = overactiveThreshold;
        this.underactiveThreshold = underactiveThreshold;
        this.plasticityStep = plasticityStep;
    }

    public void reduceSlasticityRate(String regionId) {
        double current = plasticityRates.getOrDefault(regionId, 0.01);
        plasticityRates.put(regionId, Math.max(0.0, current - plasticityStep));
    }

    public void increaseSlasticityRate(String regionId) {
        double current = plasticityRates.getOrDefault(regionId, 0.01);
        plasticityRates.put(regionId, Math.min(0.1, current + plasticityStep));
    }

    public Map<String, Double> getActivityHistory() { return activityHistory; }
    public void setActivityHistory(Map<String, Double> activityHistory) { this.activityHistory = activityHistory; }

    public Map<String, Double> getPlasticityRates() { return plasticityRates; }
    public void setPlasticityRates(Map<String, Double> plasticityRates) { this.plasticityRates = plasticityRates; }

    public double getOveractiveThreshold() { return overactiveThreshold; }
    public void setOveractiveThreshold(double overactiveThreshold) { this.overactiveThreshold = overactiveThreshold; }

    public double getUnderactiveThreshold() { return underactiveThreshold; }
    public void setUnderactiveThreshold(double underactiveThreshold) { this.underactiveThreshold = underactiveThreshold; }

    public double getPlasticityStep() { return plasticityStep; }
    public void setPlasticityStep(double plasticityStep) { this.plasticityStep = plasticityStep; }
}
