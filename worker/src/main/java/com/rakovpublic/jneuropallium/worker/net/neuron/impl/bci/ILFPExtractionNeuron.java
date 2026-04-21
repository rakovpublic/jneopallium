package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal;

public interface ILFPExtractionNeuron extends IModulatableNeuron {
    LFPSignal extract(int channelId, double[] window, long timestampNs);
}
