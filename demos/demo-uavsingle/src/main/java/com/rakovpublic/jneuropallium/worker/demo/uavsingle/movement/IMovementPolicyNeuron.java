package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

/**
 * Capability interface for the neuron that owns the autonomous movement policy network.
 * Heterogeneous movement processors are bound to this interface (mirrors
 * {@code IRecognitionNetworkNeuron} in the recognition module), so the framework can route
 * {@link MovementObservationSignal}s to the policy regardless of the concrete neuron class.
 */
public interface IMovementPolicyNeuron extends INeuron {
    MovementPolicyNetwork getNetwork();
}
