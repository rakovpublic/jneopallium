package com.rakovpublic.jneuropallium.ai.neurons.base;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public interface IModulatableNeuron extends INeuron {
    double getDopamineLevel();
    void setDopamineLevel(double dopamineLevel);
    double getErrorDampeningFactor();
    void setErrorDampeningFactor(double errorDampeningFactor);
    double getNorepinephrineLevel();
    void setNorepinephrineLevel(double norepinephrineLevel);
    double getAchLevel();
    void setAchLevel(double achLevel);
    double getInhibitionLevel();
    void setInhibitionLevel(double inhibitionLevel);
}
