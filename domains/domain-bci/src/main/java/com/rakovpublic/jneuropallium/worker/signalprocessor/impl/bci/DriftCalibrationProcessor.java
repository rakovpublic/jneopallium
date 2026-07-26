/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ICalibrationSchedulerNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.DriftEstimateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: passes a {@link DriftEstimateSignal} into an
 * {@link ICalibrationSchedulerNeuron}. When the drift or combined
 * evaluation triggers, the scheduler returns a {@link CalibrationSignal}
 * which this processor forwards for downstream action.
 */
public class DriftCalibrationProcessor implements ISignalProcessor<DriftEstimateSignal, ICalibrationSchedulerNeuron> {

    private static final String DESCRIPTION = "Drift-driven calibration scheduling";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(DriftEstimateSignal input, ICalibrationSchedulerNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        CalibrationSignal cal = neuron.evaluate(input.getEpoch(), input.getDrift(), 0.0, false);
        if (cal != null) out.add((I) cal);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return DriftCalibrationProcessor.class; }
    @Override public Class<ICalibrationSchedulerNeuron> getNeuronClass() { return ICalibrationSchedulerNeuron.class; }
    @Override public Class<DriftEstimateSignal> getSignalClass() { return DriftEstimateSignal.class; }
}
