/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;

/**
 * Contract for neurons that emit an efference copy for each motor command
 * they intercept.
 * Biological analogue: cerebellar / premotor branch points that send a
 * forward-model copy in parallel with the descending motor command.
 */
public interface IEfferenceCopyProducer extends INeuron {

    /**
     * Produce an efference copy for the given motor command.
     *
     * @param motor intercepted motor command
     * @return efference copy signal, or {@code null} if no copy is produced
     */
    EfferenceCopySignal produceCopy(MotorCommandSignal motor);
}
