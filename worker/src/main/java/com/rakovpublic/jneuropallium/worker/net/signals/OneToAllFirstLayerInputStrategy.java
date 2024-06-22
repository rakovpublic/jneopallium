/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OneToAllFirstLayerInputStrategy implements InputInitStrategy {
    public String clazz = "com.rakovpublic.jneuropallium.worker.net.signals.OneToAllFirstLayerInputStrategy";

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    @Override
    public HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getInputs(ILayersMeta layersMeta, CopyOnWriteArrayList<ISignal> signals) {
        ILayerMeta layerMeta = layersMeta.getLayerByPosition(0);
        if (layersMeta.getLayerByPosition(0).getNeurons().size() == 1) {
            layerMeta = layersMeta.getLayerByPosition(1);
        }
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
        HashMap<Long, CopyOnWriteArrayList<ISignal>> layer = new HashMap<>();
        for (INeuron neuron : layerMeta.getNeurons()) {
            CopyOnWriteArrayList<ISignal> neuronSignals = new CopyOnWriteArrayList<>();
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
