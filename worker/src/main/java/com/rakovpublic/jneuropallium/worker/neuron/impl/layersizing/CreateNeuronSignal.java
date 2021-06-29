package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.AbstractSignal;

public class CreateNeuronSignal extends AbstractSignal<INeuron> implements ISignal<INeuron> {
    public CreateNeuronSignal(INeuron value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description);

    }

    @Override
    public Class<? extends ISignal<INeuron>> getCurrentSignalClass() {
        return CreateNeuronSignal.class;
    }

    @Override
    public Class<INeuron> getParamClass() {
        return INeuron.class;
    }
}
