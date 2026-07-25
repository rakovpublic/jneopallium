package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

public interface IPacingNeuron extends IModulatableNeuron {
    int computeRatio(FlowStateKind state, DifficultyLevel difficulty);
    void setFastSlowRatioMin(int v);
    void setFastSlowRatioMax(int v);
    int getCurrentRatio();
    int getFastSlowRatioMin();
    int getFastSlowRatioMax();
}
