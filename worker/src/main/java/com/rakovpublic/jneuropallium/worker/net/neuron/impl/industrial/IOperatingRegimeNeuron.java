/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

public interface IOperatingRegimeNeuron extends IModulatableNeuron {
    OperatingRegimeSignal classify(String assetId, double rpm, double loadFraction, double flow,
                                   double pressure, double temperature, double actuatorCommand,
                                   long timestamp);
    OperatingRegimeSignal observe(String assetId, MeasurementSignal measurement);
}
