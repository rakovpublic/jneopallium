/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.embodiment;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment.IBodySchemaNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.BodySchemaUpdateSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor that applies a {@link BodySchemaUpdateSignal} to an
 * {@link IBodySchemaNeuron}: registers or damages the target effector as
 * directed by the signal.
 */
public class BodySchemaUpdateProcessor implements ISignalProcessor<BodySchemaUpdateSignal, IBodySchemaNeuron> {

    private static final String DESCRIPTION = "Applies body-schema updates (damage / tool incorporation)";

    @Override
    public <I extends ISignal> List<I> process(BodySchemaUpdateSignal input, IBodySchemaNeuron neuron) {
        if (input != null && neuron != null) neuron.onUpdate(input);
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return BodySchemaUpdateProcessor.class; }
    @Override public Class<IBodySchemaNeuron> getNeuronClass() { return IBodySchemaNeuron.class; }
    @Override public Class<BodySchemaUpdateSignal> getSignalClass() { return BodySchemaUpdateSignal.class; }
}
