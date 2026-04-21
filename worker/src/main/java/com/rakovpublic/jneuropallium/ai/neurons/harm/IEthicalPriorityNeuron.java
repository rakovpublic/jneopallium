package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraint;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.List;

public interface IEthicalPriorityNeuron extends IModulatableNeuron {
    List<AbsoluteConstraint> getHardConstraints();
    void setHardConstraints(List<AbsoluteConstraint> hardConstraints);
}
