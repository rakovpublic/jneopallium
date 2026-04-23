/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IBodySchemaNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.SensorimotorContingencySignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: forwards a {@link SensorimotorContingencySignal} to
 * an {@link IBodySchemaNeuron} via its default {@code onContingency}
 * method. Implementations that care refine their schema accordingly;
 * the default is a no-op, so enabling this processor is always safe.
 */
public class SensorimotorContingencyProcessor implements ISignalProcessor<SensorimotorContingencySignal, IBodySchemaNeuron> {

    private static final String DESCRIPTION = "Feeds action↔sensory-delta pairings into the body-schema neuron";

    @Override
    public <I extends ISignal> List<I> process(SensorimotorContingencySignal input, IBodySchemaNeuron neuron) {
        if (input != null && neuron != null) neuron.onContingency(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SensorimotorContingencyProcessor.class; }
    @Override public Class<IBodySchemaNeuron> getNeuronClass() { return IBodySchemaNeuron.class; }
    @Override public Class<SensorimotorContingencySignal> getSignalClass() { return SensorimotorContingencySignal.class; }
}
