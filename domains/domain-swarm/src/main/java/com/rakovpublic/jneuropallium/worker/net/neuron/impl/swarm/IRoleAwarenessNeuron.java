package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.Map;

public interface IRoleAwarenessNeuron extends IModulatableNeuron {
    AgentRole ownRole();
    void setOwnRole(AgentRole r);
    void onPeerState(PeerStateSignal s);
    /** Snapshot of role counts in the local neighbourhood. */
    Map<AgentRole, Integer> distribution();
    /** Returns the under-represented role the agent is biased toward, or null when balanced. */
    AgentRole shortageRole();
}
