package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public interface IMeshRadioNeuron extends IModulatableNeuron {
    /** Decide whether to forward the signal under current link quality. */
    boolean send(ISignal s, double linkQuality);
    long getSent();
    long getDropped();
    void setLossThreshold(double t);
    double getLossThreshold();
}
