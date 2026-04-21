package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal;

public interface IAmygdalaValenceNeuron extends IModulatableNeuron, IAffectiveNeuron {
    double tag(double spikeMagnitude, double threatCue);
    AffectState currentState();
    void modulateThreshold(double arousalFactor);
    void onAppraisal(AppraisalSignal s);
    double getValence();
    double getArousal();
    double getFiringThreshold();
    double getBaselineThreshold();
}
