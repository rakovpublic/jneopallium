/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

/**
 * Layer 0 sensor reader. Quality-aware; emits one {@link MeasurementSignal}
 * per accepted sample. Biological analogue: peripheral receptor.
 * Loop=1 / Epoch=1.
 */
public class SensorNeuron extends ModulatableNeuron implements ISensorNeuron {

    private long reads;

    public SensorNeuron() { super(); }
    public SensorNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public MeasurementSignal read(String tag, double value, Quality quality, long timestamp) {
        reads++;
        return new MeasurementSignal(tag, value, quality, timestamp);
    }

    @Override public long getReads() { return reads; }
}
