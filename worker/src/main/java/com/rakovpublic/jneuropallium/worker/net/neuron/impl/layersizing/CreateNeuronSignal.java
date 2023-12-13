package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class CreateNeuronSignal extends AbstractSignal<NewNeuronIntegration> implements ISignal<NewNeuronIntegration> {
    public CreateNeuronSignal(NewNeuronIntegration value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, true, true, CreateNeuronSignal.class.getName());

    }

    @Override
    public Class<? extends ISignal<NewNeuronIntegration>> getCurrentSignalClass() {
        return CreateNeuronSignal.class;
    }

    @Override
    public Class<NewNeuronIntegration> getParamClass() {
        return NewNeuronIntegration.class;
    }

    @Override
    public CreateNeuronSignal copySignal() {
        return new CreateNeuronSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(), this.getTimeAlive(), getInputName(), isFromExternalNet(), getDescription());
    }
}
