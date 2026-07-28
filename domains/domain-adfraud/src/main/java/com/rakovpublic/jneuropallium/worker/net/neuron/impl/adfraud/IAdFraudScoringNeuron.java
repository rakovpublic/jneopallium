/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IAdFraudScoringNeuron extends INeuron {
    AdFraudDecision score(AdFraudEvent event);
    void resetRuntimeState();
}
