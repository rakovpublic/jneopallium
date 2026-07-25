/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IMaintenanceSchedulingNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DegradationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MaintenanceWindowSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards a {@link DegradationSignal} into the
 * maintenance scheduler and emits the proposed window.
 */
public class DegradationSchedulingProcessor implements ISignalProcessor<DegradationSignal, IMaintenanceSchedulingNeuron> {

    private static final String DESCRIPTION = "RUL-driven maintenance scheduling";

    private long leadTimeTicks = 10_000L;

    public void setLeadTimeTicks(long t) { this.leadTimeTicks = Math.max(0L, t); }
    public long getLeadTimeTicks() { return leadTimeTicks; }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(DegradationSignal input, IMaintenanceSchedulingNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        MaintenanceWindowSignal w = neuron.schedule(input, input.getEpoch(), leadTimeTicks);
        if (w != null) out.add((I) w);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return DegradationSchedulingProcessor.class; }
    @Override public Class<IMaintenanceSchedulingNeuron> getNeuronClass() { return IMaintenanceSchedulingNeuron.class; }
    @Override public Class<DegradationSignal> getSignalClass() { return DegradationSignal.class; }
}
