package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.model.InternalForwardModel;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

public interface IPredictiveNeuron extends IModulatableNeuron {
    InternalForwardModel getModel();
    void setModel(InternalForwardModel model);
    double[] getLastPrediction();
    void setLastPrediction(double[] lastPrediction);
}
