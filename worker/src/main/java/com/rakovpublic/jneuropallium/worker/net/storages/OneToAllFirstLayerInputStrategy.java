package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class OneToAllFirstLayerInputStrategy implements InputInitStrategy {


    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getInputs(ILayersMeta layersMeta, List<ISignal> signals) {
        ILayerMeta layerMeta = layersMeta.getLayerByID(0);
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = new HashMap<>();
        HashMap<Long, List<ISignal>> layer = new HashMap<>();
        for (INeuron neuron : layerMeta.getNeurons()) {
            List<ISignal> neuronSignals = new LinkedList<>();
            for (ISignal signal : signals) {
                if (neuron.canProcess(signal)) {
                    neuronSignals.add(signal);
                }
            }
            layer.put(neuron.getId(), neuronSignals);
        }
        result.put(0, layer);
        return result;
    }
}
