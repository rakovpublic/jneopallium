/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.ICrossSensorReconstructionNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.AssetTelemetrySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal;

import java.util.LinkedList;
import java.util.List;

/** Runs the self-supervised cross-sensor reconstruction on a telemetry frame. */
public class CrossSensorReconstructionProcessor
        implements ISignalProcessor<AssetTelemetrySignal, ICrossSensorReconstructionNeuron> {

    private static final String DESCRIPTION = "Cross-sensor reconstruction residual";

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AssetTelemetrySignal input, ICrossSensorReconstructionNeuron neuron) {
        List<I> out = new LinkedList<>();
        if (input == null || neuron == null) return out;
        ReconResidualSignal residual = neuron.reconstruct(input);
        if (residual != null) out.add((I) residual);
        return out;
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return CrossSensorReconstructionProcessor.class; }
    @Override public Class<ICrossSensorReconstructionNeuron> getNeuronClass() { return ICrossSensorReconstructionNeuron.class; }
    @Override public Class<AssetTelemetrySignal> getSignalClass() { return AssetTelemetrySignal.class; }
}
