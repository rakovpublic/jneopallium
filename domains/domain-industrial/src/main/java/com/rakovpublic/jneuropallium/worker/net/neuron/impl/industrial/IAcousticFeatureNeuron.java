/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;

public interface IAcousticFeatureNeuron extends IModulatableNeuron {
    MachineFeatureSignal extract(MachineWaveformSignal waveform);
    void seedBaseline(String assetId, double rms);
    double baselineRms(String assetId);
}
