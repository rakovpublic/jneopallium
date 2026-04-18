/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.EfferenceCopySignal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Intercepts every {@link MotorCommandSignal} between {@code PlanningNeuron}
 * and {@code HarmGateNeuron} and emits an {@link EfferenceCopySignal} in
 * parallel.
 * Layer 4 (planning boundary), loop=1 / epoch=1.
 * <p>Biological analogue: the classic branch-point of a descending motor
 * command to the cerebellar forward model (Wolpert 1998).
 */
public class EfferenceCopyNeuron extends ModulatableNeuron implements IEfferenceCopyProducer {

    private static final AtomicLong COMMAND_COUNTER = new AtomicLong(0);

    public EfferenceCopyNeuron() { super(); }

    public EfferenceCopyNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    @Override
    public EfferenceCopySignal produceCopy(MotorCommandSignal motor) {
        if (motor == null) return null;
        long cmdId = COMMAND_COUNTER.incrementAndGet();
        double[] predicted = motor.getParams() == null ? new double[0] : motor.getParams().clone();
        EfferenceCopySignal copy = new EfferenceCopySignal(cmdId, predicted, motor.getEffectorId());
        copy.setSourceNeuronId(this.getId());
        return copy;
    }
}
