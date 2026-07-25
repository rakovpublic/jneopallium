package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.InflammationBroadcastSignal;

public interface IEntityBehaviourBaselineNeuron extends IModulatableNeuron {
    /** Update EWMA baseline feature vector for {@code entityId}. No-op when frozen by inflammation. */
    void update(String entityId, double[] featureVector);
    double[] baselineFor(String entityId);
    void onInflammation(InflammationBroadcastSignal s);
    boolean isFrozen();
    void setWindowTicks(long t);
    long getWindowTicks();
}
