package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;

public interface IPeerObservationNeuron extends IModulatableNeuron {
    PeerObservationSignal observe(String peerId, double[] positionLocal, double[] velocityLocal, double linkQuality);
    long getObservationCount();
    void setMinLinkQuality(double q);
    double getMinLinkQuality();
}
