package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ILayerManipulatingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;

import java.util.LinkedList;
import java.util.List;

public class DeleteNeuronProcessor implements ISignalProcessor<DeleteNeuronSignal, ILayerManipulatingNeuron> {
    @Override
    public <I extends ISignal> List<I> process(DeleteNeuronSignal input, ILayerManipulatingNeuron neuron) {
        neuron.getLayer().deleteNeuron(input);
        return new LinkedList<>();
    }

    @Override
    public String getDescription() {
        return "demo neuron to show how manipulate the layer";
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return DeleteNeuronProcessor.class;
    }

    @Override
    public Class<ILayerManipulatingNeuron> getNeuronClass() {
        return ILayerManipulatingNeuron.class;
    }
}
