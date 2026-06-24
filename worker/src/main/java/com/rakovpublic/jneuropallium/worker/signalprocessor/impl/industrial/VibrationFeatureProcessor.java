/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IVibrationFeatureNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineWaveformSignal;

import java.util.LinkedList;
import java.util.List;

/** Converts vibration waveform frames into compressed machine features. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VibrationFeatureProcessor implements ISignalProcessor<MachineWaveformSignal, IVibrationFeatureNeuron> {

    private static final String DESCRIPTION = "Vibration waveform feature extraction";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MachineWaveformSignal input, IVibrationFeatureNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        if (!MachineWaveformSignal.CHANNEL_VIBRATION.equals(input.getChannel())) return out;
        MachineFeatureSignal feature = neuron.extract(input);
        if (feature != null) out.add((I) feature);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return VibrationFeatureProcessor.class; }
    @Override public Class<IVibrationFeatureNeuron> getNeuronClass() { return IVibrationFeatureNeuron.class; }
    @Override public Class<MachineWaveformSignal> getSignalClass() { return MachineWaveformSignal.class; }
}
