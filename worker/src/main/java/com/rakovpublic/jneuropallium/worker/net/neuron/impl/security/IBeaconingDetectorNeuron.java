package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.AnomalyScoreSignal;

public interface IBeaconingDetectorNeuron extends IModulatableNeuron {
    /** Observe one connection event for {@code entityId} at {@code tick} (monotonic). */
    void observe(String entityId, long tick);
    /** Returns an anomaly score signal when beacon periodicity is detected; null otherwise. */
    AnomalyScoreSignal assess(String entityId);
    void setMinSamples(int n);
    int getMinSamples();
    void setJitterTolerance(double j);
    double getJitterTolerance();
}
