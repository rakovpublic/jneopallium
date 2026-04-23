/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IThermalMonitorNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: feeds a {@link ThermalSignal} into an
 * {@link IThermalMonitorNeuron}, which may transition into cool-down
 * or shutdown state. The signal is re-emitted only when the thermal
 * budget has been exceeded so downstream neurons can react.
 */
public class ThermalMonitorProcessor implements ISignalProcessor<ThermalSignal, IThermalMonitorNeuron> {

    private static final String DESCRIPTION = "Implant thermal-budget monitor";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(ThermalSignal input, IThermalMonitorNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        boolean exceeded = neuron.observe(input);
        if (exceeded) out.add((I) input);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ThermalMonitorProcessor.class; }
    @Override public Class<IThermalMonitorNeuron> getNeuronClass() { return IThermalMonitorNeuron.class; }
    @Override public Class<ThermalSignal> getSignalClass() { return ThermalSignal.class; }
}
