/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.ICalibrationSchedulerNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: records an external calibration event against an
 * {@link ICalibrationSchedulerNeuron} so the min-interval rule honours
 * it. Emits no follow-up signals.
 */
public class CalibrationReportProcessor implements ISignalProcessor<CalibrationSignal, ICalibrationSchedulerNeuron> {

    private static final String DESCRIPTION = "External calibration-event observer";

    @Override
    public <I extends ISignal> List<I> process(CalibrationSignal input, ICalibrationSchedulerNeuron neuron) {
        if (input != null && neuron != null) neuron.observeCalibration(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return CalibrationReportProcessor.class; }
    @Override public Class<ICalibrationSchedulerNeuron> getNeuronClass() { return ICalibrationSchedulerNeuron.class; }
    @Override public Class<CalibrationSignal> getSignalClass() { return CalibrationSignal.class; }
}
