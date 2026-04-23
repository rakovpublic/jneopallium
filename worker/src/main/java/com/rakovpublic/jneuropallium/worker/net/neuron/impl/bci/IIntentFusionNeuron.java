package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal;

public interface IIntentFusionNeuron extends IModulatableNeuron {
    IntentSignal fuse(IntentSignal spikeIntent, IntentSignal lfpIntent);
    void setSpikeWeight(double w);
    void setLfpWeight(double w);
    double getSpikeWeight();
    double getLfpWeight();

    /**
     * Observation channel: a single intent stream buffers until a
     * complementary stream is paired and fused. Default implementation
     * holds the most recent intent in memory and fuses on arrival of a
     * second one.
     */
    default IntentSignal observe(IntentSignal s) { return s; }
}
