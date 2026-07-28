package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Deque;

public interface IRegionMonitorNeuron extends IModulatableNeuron {
    void recordFiring(double magnitude);
    double computeMean();
    double computeVariance();
    double computeTrend();
    String getMonitoredRegion();
    void setMonitoredRegion(String monitoredRegion);
    Deque<Double> getFiringHistory();
    void setFiringHistory(Deque<Double> firingHistory);
    int getRingBufferCapacity();
    void setRingBufferCapacity(int ringBufferCapacity);
}
