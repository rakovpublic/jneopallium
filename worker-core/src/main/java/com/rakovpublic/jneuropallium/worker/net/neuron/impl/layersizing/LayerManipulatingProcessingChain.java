package com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class LayerManipulatingProcessingChain implements ISignalChain {
    private final List<Class<? extends ISignal>> order;
    private static final String description = "order for layer sizing signals processing";

    public LayerManipulatingProcessingChain() {
        order = new LinkedList<>();
        order.add(DeleteNeuronSignal.class);
        order.add(CreateNeuronSignal.class);
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        return order;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
