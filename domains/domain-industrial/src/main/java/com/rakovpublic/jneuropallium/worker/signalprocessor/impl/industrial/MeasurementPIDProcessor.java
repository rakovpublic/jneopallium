/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IPIDNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: drives a PID step from a fresh measurement and
 * forwards the resulting actuator command. Loop period is inferred
 * from the measurement's epoch delta via a configurable default.
 */
public class MeasurementPIDProcessor implements ISignalProcessor<MeasurementSignal, IPIDNeuron> {

    private static final String DESCRIPTION = "PID controller measurement→actuator step";

    private double dtSeconds = 0.01;

    public void setDtSeconds(double dt) { this.dtSeconds = Math.max(1e-6, dt); }
    public double getDtSeconds() { return dtSeconds; }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MeasurementSignal input, IPIDNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ActuatorCommandSignal cmd = neuron.step(input, dtSeconds);
        if (cmd != null) out.add((I) cmd);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MeasurementPIDProcessor.class; }
    @Override public Class<IPIDNeuron> getNeuronClass() { return IPIDNeuron.class; }
    @Override public Class<MeasurementSignal> getSignalClass() { return MeasurementSignal.class; }
}
