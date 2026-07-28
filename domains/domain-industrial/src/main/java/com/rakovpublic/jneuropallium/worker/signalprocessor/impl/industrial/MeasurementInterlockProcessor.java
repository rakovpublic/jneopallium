/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IInterlockNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: evaluates the hard-wired interlock rules against
 * every incoming measurement and forwards any trip signals. Fail-safe
 * actuator commands produced by the same evaluation are retrieved
 * separately by the caller via {@link IInterlockNeuron#failSafeCommands()}.
 */
public class MeasurementInterlockProcessor implements ISignalProcessor<MeasurementSignal, IInterlockNeuron> {

    private static final String DESCRIPTION = "Hard-wired interlock evaluation";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MeasurementSignal input, IInterlockNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        List<InterlockSignal> trips = neuron.evaluate(input);
        if (trips != null) for (InterlockSignal t : trips) if (t != null) out.add((I) t);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MeasurementInterlockProcessor.class; }
    @Override public Class<IInterlockNeuron> getNeuronClass() { return IInterlockNeuron.class; }
    @Override public Class<MeasurementSignal> getSignalClass() { return MeasurementSignal.class; }
}
