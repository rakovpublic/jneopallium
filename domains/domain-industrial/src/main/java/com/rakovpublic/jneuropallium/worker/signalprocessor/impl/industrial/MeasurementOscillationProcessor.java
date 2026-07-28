/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IOscillationMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds a {@link MeasurementSignal} into the
 * oscillation monitor's ACF window. Emits nothing; the intervention
 * band is read on demand by a supervisory controller.
 */
public class MeasurementOscillationProcessor implements ISignalProcessor<MeasurementSignal, IOscillationMonitorNeuron> {

    private static final String DESCRIPTION = "ACF oscillation monitor update";

    @Override
    public <I extends ISignal> List<I> process(MeasurementSignal input, IOscillationMonitorNeuron neuron) {
        if (input != null && neuron != null) neuron.observe(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MeasurementOscillationProcessor.class; }
    @Override public Class<IOscillationMonitorNeuron> getNeuronClass() { return IOscillationMonitorNeuron.class; }
    @Override public Class<MeasurementSignal> getSignalClass() { return MeasurementSignal.class; }
}
