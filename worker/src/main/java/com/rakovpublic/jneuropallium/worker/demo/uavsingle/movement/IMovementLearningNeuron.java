package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

/**
 * Capability interface for the neuron that applies reinforcement to the movement policy.
 * Mirrors {@code IRecognitionLearningNeuron}: a {@link MovementReinforcementSignal} carrying a
 * scalar reward updates the dendrite weights of the action neuron that produced the last command.
 */
public interface IMovementLearningNeuron extends INeuron {
    MovementPolicyNetwork getNetwork();

    MovementLearningResultSignal applyReward(MovementReinforcementSignal reinforcement);
}
