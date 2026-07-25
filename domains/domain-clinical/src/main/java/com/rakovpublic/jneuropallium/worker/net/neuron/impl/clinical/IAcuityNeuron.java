package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

public interface IAcuityNeuron extends IModulatableNeuron {
    void ingest(VitalSignal v);
    double score();
    int rawScore();
    double harmThresholdMultiplier();
    void reset();
}
