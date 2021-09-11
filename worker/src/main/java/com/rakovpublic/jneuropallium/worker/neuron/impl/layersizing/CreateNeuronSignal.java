package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.AbstractSignal;

public class CreateNeuronSignal extends AbstractSignal<NewNeuronIntegration> implements ISignal<NewNeuronIntegration> {
    public CreateNeuronSignal(NewNeuronIntegration value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description);

    }

    @Override
    public Class<? extends ISignal<NewNeuronIntegration>> getCurrentSignalClass() {
        return CreateNeuronSignal.class;
    }

    @Override
    public Class<NewNeuronIntegration> getParamClass() {
        return NewNeuronIntegration.class;
    }
}
