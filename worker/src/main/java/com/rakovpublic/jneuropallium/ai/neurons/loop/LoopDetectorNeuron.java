package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ActivityMeasurementSignal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loop detector neuron used by ActivityAnalysisProcessor.
 * Maintains a sliding-window history of activity measurements per region
 * to detect positive and negative runaway loops.
 */
public class LoopDetectorNeuron extends ModulatableNeuron implements ILoopDetectorNeuron {

    private Map<String, List<ActivityMeasurementSignal>> regionHistory;
    private Map<String, Double> baselineRates;
    private int historyWindowSize;

    public LoopDetectorNeuron() {
        super();
        this.regionHistory = new HashMap<>();
        this.baselineRates = new HashMap<>();
        this.historyWindowSize = 10;
    }

    public LoopDetectorNeuron(Long neuronId,
                              com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                              Long run,
                              int historyWindowSize) {
        super(neuronId, chain, run);
        this.regionHistory = new HashMap<>();
        this.baselineRates = new HashMap<>();
        this.historyWindowSize = historyWindowSize;
    }

    public double getBaselineRate(String regionId) {
        return baselineRates.getOrDefault(regionId, 50.0);
    }

    public void setBaselineRate(String regionId, double rate) {
        baselineRates.put(regionId, rate);
    }

    public Map<String, List<ActivityMeasurementSignal>> getRegionHistory() { return regionHistory; }
    public void setRegionHistory(Map<String, List<ActivityMeasurementSignal>> regionHistory) { this.regionHistory = regionHistory; }

    public Map<String, Double> getBaselineRates() { return baselineRates; }
    public void setBaselineRates(Map<String, Double> baselineRates) { this.baselineRates = baselineRates; }

    public int getHistoryWindowSize() { return historyWindowSize; }
    public void setHistoryWindowSize(int historyWindowSize) { this.historyWindowSize = historyWindowSize; }
}
