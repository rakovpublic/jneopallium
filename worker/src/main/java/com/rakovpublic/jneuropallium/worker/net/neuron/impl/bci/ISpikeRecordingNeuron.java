package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;

public interface ISpikeRecordingNeuron extends IModulatableNeuron {
    void setChannelId(int c);
    int getChannelId();
    void setThresholdUV(double t);
    double getThresholdUV();
    NeuralSpikeSignal detect(double[] samples, long startTsNs, long sampleIntervalNs);
}
