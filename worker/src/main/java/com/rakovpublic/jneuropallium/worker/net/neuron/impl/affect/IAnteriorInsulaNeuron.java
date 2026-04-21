package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal;

public interface IAnteriorInsulaNeuron extends IModulatableNeuron, IInteroceptive {
    void integrate(InteroceptiveSignal s);
    double readHomeostaticError();
    double readEnergyBudget();
    double getAveragedPain();
    int getSampleCount();
}
