package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ActivityMeasurementSignal;

import java.util.List;
import java.util.Map;

public interface ILoopDetectorNeuron extends IModulatableNeuron {
    double getBaselineRate(String regionId);
    void setBaselineRate(String regionId, double rate);
    Map<String, List<ActivityMeasurementSignal>> getRegionHistory();
    void setRegionHistory(Map<String, List<ActivityMeasurementSignal>> regionHistory);
    Map<String, Double> getBaselineRates();
    void setBaselineRates(Map<String, Double> baselineRates);
    int getHistoryWindowSize();
    void setHistoryWindowSize(int historyWindowSize);
}
