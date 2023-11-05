package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.AbstractSignal;

public class DeleteNeuronSignal extends AbstractSignal<DeleteNeuronIntegration> implements ISignal<DeleteNeuronIntegration> {
    public DeleteNeuronSignal(DeleteNeuronIntegration value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, true, true, DeleteNeuronSignal.class.getName());
    }

    @Override
    public Class<? extends ISignal<DeleteNeuronIntegration>> getCurrentSignalClass() {
        return DeleteNeuronSignal.class;
    }

    @Override
    public Class<DeleteNeuronIntegration> getParamClass() {
        return DeleteNeuronIntegration.class;
    }

    @Override
    public DeleteNeuronSignal copySignal() {
        return new DeleteNeuronSignal(value,this.getSourceLayerId(),this.getSourceNeuronId(),this.getTimeAlive(),getInputName(),isFromExternalNet(),getDescription());
    }
}
