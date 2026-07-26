/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.IWaveformAnalysisNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: hands a {@link WaveformSignal} buffer to an
 * {@link IWaveformAnalysisNeuron} for ECG/PPG/EEG feature extraction
 * and forwards any pathological alert it produces.
 */
public class WaveformAnalysisProcessor implements ISignalProcessor<WaveformSignal, IWaveformAnalysisNeuron> {

    private static final String DESCRIPTION = "Real-time waveform classifier";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(WaveformSignal input, IWaveformAnalysisNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        AdverseEventAlertSignal alert = neuron.analyse(input);
        if (alert != null) out.add((I) alert);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return WaveformAnalysisProcessor.class; }
    @Override public Class<IWaveformAnalysisNeuron> getNeuronClass() { return IWaveformAnalysisNeuron.class; }
    @Override public Class<WaveformSignal> getSignalClass() { return WaveformSignal.class; }
}
