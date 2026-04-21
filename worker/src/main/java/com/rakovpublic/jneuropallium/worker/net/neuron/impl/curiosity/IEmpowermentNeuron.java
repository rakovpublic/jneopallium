package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.EmpowermentSignal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IEmpowermentNeuron extends IModulatableNeuron {
    EmpowermentSignal estimate(int stateId, List<List<Integer>> rollouts);
    int getHorizon();
    void setHorizon(int horizon);
    int getNActionSamples();
    void setNActionSamples(int nActionSamples);
}
