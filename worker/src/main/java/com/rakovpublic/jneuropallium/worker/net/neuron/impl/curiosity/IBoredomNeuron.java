package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.BoredomSignal;

public interface IBoredomNeuron extends IModulatableNeuron {
    BoredomSignal visit(String contextHash);
    AttentionGateSignal maybeSuppress(String contextHash);
    void reset(String contextHash);
    double getFamiliarityThreshold();
    void setFamiliarityThreshold(double t);
    int getSaturationVisits();
    void setSaturationVisits(int s);
    int visitsFor(String contextHash);
}
