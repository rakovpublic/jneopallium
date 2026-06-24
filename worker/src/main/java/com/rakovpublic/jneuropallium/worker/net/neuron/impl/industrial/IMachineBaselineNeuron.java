/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;

public interface IMachineBaselineNeuron extends IModulatableNeuron {
    DomainShiftSignal observe(MachineFeatureSignal feature);
    void seedBaseline(String assetId, String featureFamily, double rms, double spectralCentroidHz, double anomalyScore);
    double baselineRms(String assetId, String featureFamily);
}
