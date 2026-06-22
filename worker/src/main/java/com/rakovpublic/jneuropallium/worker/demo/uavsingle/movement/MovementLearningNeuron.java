package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

/**
 * Neuron that reinforces the movement policy from discrete reward events (occlusion rejection,
 * missed target, accepted photo). Shares the {@link MovementPolicyNetwork} with the decision neuron,
 * exactly as {@code RecognitionLearningNeuron} shares the recognition network. Analogue of that
 * class.
 */
public class MovementLearningNeuron extends Neuron implements IMovementLearningNeuron {
    private final MovementPolicyNetwork network;

    public MovementLearningNeuron() {
        this(new MovementPolicyNetwork());
    }

    public MovementLearningNeuron(MovementPolicyNetwork network) {
        super();
        this.network = network == null ? new MovementPolicyNetwork() : network;
        this.currentNeuronClass = MovementLearningNeuron.class;
        this.resultClasses.add(MovementLearningResultSignal.class);
        addSignalProcessor(MovementReinforcementSignal.class, new MovementLearningProcessor());
    }

    @Override
    public MovementPolicyNetwork getNetwork() {
        return network;
    }

    @Override
    public MovementLearningResultSignal applyReward(MovementReinforcementSignal reinforcement) {
        MovementLearningResultSignal result = network.reinforceEvent(
                reinforcement.getReward(), reinforcement.getFrame(), reinforcement.getReason(),
                reinforcement.getExtras());
        result.setMissionId(reinforcement.getMissionId());
        result.setUavId(reinforcement.getUavId());
        result.setTick(reinforcement.getFrame());
        setChanged(result.isApplied());
        return result;
    }
}
