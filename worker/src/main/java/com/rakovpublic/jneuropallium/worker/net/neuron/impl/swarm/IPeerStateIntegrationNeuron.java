package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerStateSignal;

import java.util.Map;
import java.util.Set;

public interface IPeerStateIntegrationNeuron extends IModulatableNeuron {
    void onObservation(PeerObservationSignal o, long currentTick);
    void onState(PeerStateSignal s, long currentTick);
    /** Drop entries older than {@code stalenessTicks} from the local view. */
    int evict(long currentTick);
    Set<String> knownPeers();
    Map<String, AgentRole> roleSnapshot();
    Long lastSeen(String peerId);
    void setStalenessTicks(long t);
    long getStalenessTicks();
}
