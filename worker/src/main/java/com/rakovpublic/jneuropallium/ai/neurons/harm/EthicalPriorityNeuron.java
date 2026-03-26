package com.rakovpublic.jneuropallium.ai.neurons.harm;

import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraint;
import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraintFactory;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.Arrays;
import java.util.List;

/**
 * Ethical priority neuron used by HardConstraintProcessor.
 * Holds a list of absolute (monotonically-escalating) safety constraints.
 * A constraint can only raise a harm verdict — it can never lower it.
 */
public class EthicalPriorityNeuron extends ModulatableNeuron {

    private List<AbsoluteConstraint> hardConstraints;

    public EthicalPriorityNeuron() {
        super();
        this.hardConstraints = Arrays.asList(
            AbsoluteConstraintFactory.physicalFatalityConstraint(),
            AbsoluteConstraintFactory.informationDeceptionConstraint(),
            AbsoluteConstraintFactory.safetySystemTamperingConstraint()
        );
    }

    public EthicalPriorityNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 List<AbsoluteConstraint> hardConstraints) {
        super(neuronId, chain, run);
        this.hardConstraints = hardConstraints;
    }

    public List<AbsoluteConstraint> getHardConstraints() { return hardConstraints; }
    public void setHardConstraints(List<AbsoluteConstraint> hardConstraints) { this.hardConstraints = hardConstraints; }
}
