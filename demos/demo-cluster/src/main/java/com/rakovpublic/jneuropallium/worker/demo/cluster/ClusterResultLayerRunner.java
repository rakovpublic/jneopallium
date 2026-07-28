package com.rakovpublic.jneuropallium.worker.demo.cluster;

import com.rakovpublic.jneuropallium.worker.net.core.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Turns the signals that reached the result layer into result neurons.
 * <p>
 * The master reads the result layer from the shared store, so the neurons it gets are fresh
 * copies without the in-memory processing state; the signals are replayed into them here.
 */
public class ClusterResultLayerRunner implements IResultLayerRunner {

    @Override
    public List<? extends IResultNeuron> getResults(IResultLayerMeta resultLayer,
                                                   HashMap<Long, CopyOnWriteArrayList<ISignal>> signals) {
        List<IResultNeuron> results = new ArrayList<>();
        if (resultLayer == null) {
            return results;
        }
        for (IResultNeuron neuron : resultLayer.getResultNeurons()) {
            CopyOnWriteArrayList<ISignal> neuronSignals = signals == null ? null : signals.get(neuron.getId());
            if (neuronSignals == null || neuronSignals.isEmpty()) {
                continue;
            }
            neuron.addSignals(new ArrayList<>(neuronSignals));
            neuron.processSignals();
            results.add(neuron);
        }
        return results;
    }
}
