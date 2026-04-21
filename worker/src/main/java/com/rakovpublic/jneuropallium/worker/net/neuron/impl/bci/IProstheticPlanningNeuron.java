package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

public interface IProstheticPlanningNeuron extends IModulatableNeuron {
    void setDof(int d);
    void setJointLimits(int j, double lo, double hi);
    void setCurrentJoints(double[] q);
    double[] step(double[] targetJoints, double maxDelta);
    double[] getCurrentJoints();
    int getDof();
}
