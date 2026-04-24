/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IAlarmAggregationNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: passes a raw {@link AlarmSignal} through the
 * ISA-18.2 aggregator. Suppressed standing alarms are dropped; the
 * rest are forwarded.
 */
public class AlarmAggregationProcessor implements ISignalProcessor<AlarmSignal, IAlarmAggregationNeuron> {

    private static final String DESCRIPTION = "ISA-18.2 alarm aggregation and suppression";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AlarmSignal input, IAlarmAggregationNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        AlarmSignal forwarded = neuron.observe(input);
        if (forwarded != null) out.add((I) forwarded);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return AlarmAggregationProcessor.class; }
    @Override public Class<IAlarmAggregationNeuron> getNeuronClass() { return IAlarmAggregationNeuron.class; }
    @Override public Class<AlarmSignal> getSignalClass() { return AlarmSignal.class; }
}
