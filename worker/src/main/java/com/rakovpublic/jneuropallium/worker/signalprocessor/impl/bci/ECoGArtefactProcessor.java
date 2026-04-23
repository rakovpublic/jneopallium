/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IArtefactRejectionNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ECoGSignal;

import java.util.LinkedList;
import java.util.List;

/**
 * Stateless processor: checks a one-sample {@link ECoGSignal} through an
 * {@link IArtefactRejectionNeuron}, which may mask the channel if the
 * amplitude exceeds its configured limit. Emits no follow-up signals.
 */
public class ECoGArtefactProcessor implements ISignalProcessor<ECoGSignal, IArtefactRejectionNeuron> {

    private static final String DESCRIPTION = "ECoG amplitude artefact rejection";

    @Override
    public <I extends ISignal> List<I> process(ECoGSignal input, IArtefactRejectionNeuron neuron) {
        if (input != null && neuron != null) {
            neuron.check(input.getElectrodeId(), new double[]{input.getVoltage()});
        }
        return new LinkedList<>();
    }

    @Override public String getDescription() { return DESCRIPTION; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ECoGArtefactProcessor.class; }
    @Override public Class<IArtefactRejectionNeuron> getNeuronClass() { return IArtefactRejectionNeuron.class; }
    @Override public Class<ECoGSignal> getSignalClass() { return ECoGSignal.class; }
}
