package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ScaffoldingSignal;

public interface IScaffoldingNeuron extends IModulatableNeuron {
    ScaffoldingSignal scaffoldFor(FlowStateKind state, String conceptId);
}
