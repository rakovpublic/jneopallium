/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

public interface IMachineHealthCorrelationNeuron extends IModulatableNeuron {
    MachineHealthAdvisorySignal correlate(FaultHypothesisSignal hypothesis);
    void setModelVersion(String modelVersion);
    String getModelVersion();
}
