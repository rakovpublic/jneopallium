package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

/**
 * Neuron that owns the movement policy and turns a {@link MovementObservationSignal} into a
 * {@link MovementDecisionSignal} + {@link MotorCommand}. Analogue of {@code ImageRecognitionNeuron}.
 */
public class MovementDecisionNeuron extends Neuron implements IMovementPolicyNeuron {
    private final MovementPolicyNetwork network;

    public MovementDecisionNeuron() {
        this(new MovementPolicyNetwork());
    }

    public MovementDecisionNeuron(MovementPolicyNetwork network) {
        super();
        this.network = network == null ? new MovementPolicyNetwork() : network;
        this.currentNeuronClass = MovementDecisionNeuron.class;
        this.resultClasses.add(MovementDecisionSignal.class);
        this.resultClasses.add(MovementLearningResultSignal.class);
        addSignalProcessor(MovementObservationSignal.class, new MovementPolicyProcessor());
    }

    @Override
    public MovementPolicyNetwork getNetwork() {
        return network;
    }

    /** Convenience for in-process callers: select the next action for this observation. */
    public MovementDecisionSignal decide(MovementObservationSignal observation) {
        return decideOutcome(observation).getDecision();
    }

    public MovementPolicyNetwork.DecisionOutcome decideOutcome(MovementObservationSignal observation) {
        MovementPolicyNetwork.DecisionOutcome outcome = network.decide(observation);
        setChanged(true);
        return outcome;
    }
}
