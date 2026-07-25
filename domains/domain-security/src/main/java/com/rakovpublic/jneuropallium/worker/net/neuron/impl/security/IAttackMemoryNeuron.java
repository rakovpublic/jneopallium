package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;

import java.util.Set;

public interface IAttackMemoryNeuron extends IModulatableNeuron {
    /** Memoise a known TTP fingerprint keyed by {@code campaignId}. */
    void store(String campaignId, ThreatCategory category, String... ttpSignatures);
    /** Returns the set of known campaign ids that match any of the supplied ttps. */
    Set<String> lookup(String... ttpSignatures);
    int size();
    void clear();
}
