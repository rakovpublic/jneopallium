/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.IOperatingRegimeNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

import java.util.LinkedList;
import java.util.List;

/** Maintains operating-regime context from scalar measurements. */
public class OperatingRegimeProcessor implements ISignalProcessor<MeasurementSignal, IOperatingRegimeNeuron> {

    private static final String DESCRIPTION = "Operating regime context update";
    private String defaultAssetId = "P-101";

    public void setDefaultAssetId(String defaultAssetId) {
        if (defaultAssetId != null && !defaultAssetId.isBlank()) this.defaultAssetId = defaultAssetId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MeasurementSignal input, IOperatingRegimeNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        OperatingRegimeSignal regime = neuron.observe(defaultAssetId, input);
        if (regime != null) out.add((I) regime);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return OperatingRegimeProcessor.class; }
    @Override public Class<IOperatingRegimeNeuron> getNeuronClass() { return IOperatingRegimeNeuron.class; }
    @Override public Class<MeasurementSignal> getSignalClass() { return MeasurementSignal.class; }
}
