package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal;
import java.util.ArrayList;
import java.util.List;

public interface ISpikeSortingNeuron extends IModulatableNeuron {
    int sort(NeuralSpikeSignal spike);
    int unitCount();
    void setMatchThreshold(double t);
    void setMaxUnits(int n);
}
