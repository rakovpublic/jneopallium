package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

public interface ISensorNeuron extends IModulatableNeuron {
    MeasurementSignal read(String tag, double value, Quality quality, long timestamp);
    long getReads();
}
