/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IMeasurementValidatorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: passes every {@link MeasurementSignal} through
 * an {@link IMeasurementValidatorNeuron} (range / rate-of-change
 * check). The signal is always forwarded — quality may have been
 * downgraded in place — because in industrial control a dropped
 * measurement is worse than a flagged one.
 */
public class MeasurementValidationProcessor implements ISignalProcessor<MeasurementSignal, IMeasurementValidatorNeuron> {

    private static final String DESCRIPTION = "Range / rate-of-change measurement validation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MeasurementSignal input, IMeasurementValidatorNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        MeasurementSignal validated = neuron.validate(input);
        if (validated != null) out.add((I) validated);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MeasurementValidationProcessor.class; }
    @Override public Class<IMeasurementValidatorNeuron> getNeuronClass() { return IMeasurementValidatorNeuron.class; }
    @Override public Class<MeasurementSignal> getSignalClass() { return MeasurementSignal.class; }
}
