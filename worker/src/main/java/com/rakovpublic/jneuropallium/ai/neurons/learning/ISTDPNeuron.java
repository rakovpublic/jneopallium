package com.rakovpublic.jneuropallium.ai.neurons.learning;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Map;

public interface ISTDPNeuron extends IModulatableNeuron {
    void adjustWeight(String synapseId, double delta);
    Map<String, Long> getPreSpikeTimestamps();
    void setPreSpikeTimestamps(Map<String, Long> preSpikeTimestamps);
    Map<String, Double> getWeights();
    void setWeights(Map<String, Double> weights);
    long getStdpWindow();
    void setStdpWindow(long stdpWindow);
    double getLtpRate();
    void setLtpRate(double ltpRate);
    double getLtdRate();
    void setLtdRate(double ltdRate);
}
