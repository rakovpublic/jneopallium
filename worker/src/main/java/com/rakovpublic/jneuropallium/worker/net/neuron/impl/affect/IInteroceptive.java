/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

/**
 * Contract for neurons that summarise body-state telemetry.
 * Biological analogue: anterior insula and primary interoceptive cortex.
 */
public interface IInteroceptive extends INeuron {

    /**
     * @return the current homeostatic error (0 = perfect, larger = worse).
     */
    double readHomeostaticError();

    /**
     * @return the current energy budget reading (arbitrary positive units).
     */
    double readEnergyBudget();
}
