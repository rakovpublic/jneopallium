/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.DomainShiftSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.FaultHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineFeatureSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatingRegimeSignal;

public interface IFaultHypothesisNeuron extends IModulatableNeuron {
    FaultHypothesisSignal hypothesize(MachineFeatureSignal feature);
    void observeDomainShift(DomainShiftSignal domainShift);
    void observeRegime(OperatingRegimeSignal regime);
}
