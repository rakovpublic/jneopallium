package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraint;
import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraintFactory;
import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import java.util.Arrays;
import java.util.List;

public interface IEthicalPriorityNeuron extends IModulatableNeuron {
    List<AbsoluteConstraint> getHardConstraints();
    void setHardConstraints(List<AbsoluteConstraint> hardConstraints);
}
