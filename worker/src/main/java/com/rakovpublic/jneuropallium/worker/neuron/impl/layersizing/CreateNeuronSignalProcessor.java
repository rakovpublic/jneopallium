package com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.ILayerManipulatingNeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;

import java.util.LinkedList;
import java.util.List;


public class CreateNeuronSignalProcessor implements ISignalProcessor<CreateNeuronSignal, ILayerManipulatingNeuron> {
    @Override
    public <I extends ISignal> List<I> process(CreateNeuronSignal input, ILayerManipulatingNeuron neuron) {
        neuron.getLayer().createNeuron(input);
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
        return CreateNeuronSignalProcessor.class;
    }

    @Override
    public Class<ILayerManipulatingNeuron> getNeuronClass() {
        return ILayerManipulatingNeuron.class;
    }
}
