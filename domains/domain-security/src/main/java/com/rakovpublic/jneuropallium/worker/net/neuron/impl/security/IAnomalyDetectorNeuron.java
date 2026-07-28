package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

public interface IAnomalyDetectorNeuron extends IModulatableNeuron {
    AnomalyScoreSignal score(String entityId, double[] featureVector);
    void setScoreThresholdSoft(double t);
    void setScoreThresholdHard(double t);
    double getScoreThresholdSoft();
    double getScoreThresholdHard();
}
