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
 * Stateless processor: echoes an external {@link MaintenanceWindowSignal}
 * back through the scheduler (e.g. a technician-entered window) so
 * bookkeeping stays consistent. Re-emits the signal unchanged for any
 * downstream consumer.
 */
public class MaintenanceWindowSchedulingProcessor implements ISignalProcessor<MaintenanceWindowSignal, IMaintenanceSchedulingNeuron> {

    private static final String DESCRIPTION = "External maintenance-window registration";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MaintenanceWindowSignal input, IMaintenanceSchedulingNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        // Record via a synthetic DegradationSignal so the scheduler
        // increments its per-asset counter through the same code path.
        neuron.schedule(new DegradationSignal(input.getAssetId(), 0.0, 1.0),
                input.getScheduledTick(), neuron.getMinLeadTimeTicks());
        out.add((I) input);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MaintenanceWindowSchedulingProcessor.class; }
    @Override public Class<IMaintenanceSchedulingNeuron> getNeuronClass() { return IMaintenanceSchedulingNeuron.class; }
    @Override public Class<MaintenanceWindowSignal> getSignalClass() { return MaintenanceWindowSignal.class; }
}
