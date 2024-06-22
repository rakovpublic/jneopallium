package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Dendrites implements IDendrites {
    private final static Logger logger = LogManager.getLogger(Dendrites.class);

    public Dendrites() {
        weights = new HashMap<>();
        defaultDendritesWeights = new HashMap<>();
    }

    @JsonSerialize(keyUsing = NeuronAddressSerializer.class)
    @JsonDeserialize(keyUsing = NeuronAddressDeserializer.class)
    public HashMap<NeuronAddress, List<IWeight>> weights;

    public HashMap<Class<? extends ISignal>, IWeight> defaultDendritesWeights;

    public HashMap<NeuronAddress, List<IWeight>> getWeights() {
        return weights;
    }

    public void setWeights(HashMap<NeuronAddress, List<IWeight>> weights) {
        this.weights = weights;
    }

    public HashMap<Class<? extends ISignal>, IWeight> getDefaultDendritesWeights() {
        return defaultDendritesWeights;
    }

    @Override
    public void setDefaultDendritesWeights(HashMap<Class<? extends ISignal>, IWeight> defaultDendritesWeights) {
        this.defaultDendritesWeights = defaultDendritesWeights;
    }

    @Override
    public void updateWeight(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass, IWeight weight) {
        if (weights == null) {
            weights = new HashMap<>();
        }
        if (weights.containsKey(neuronAddress)) {
            weights.get(neuronAddress).add(weight);
        } else {
            List<IWeight> weightMapping = new LinkedList<>();
            weightMapping.add(weight);
            weights.put(neuronAddress, weightMapping);
        }

    }

    @Override
    public List<ISignal> processSignalsWithDendrites(List<ISignal> signals) {
        return signals.parallelStream().map(signal -> {
            NeuronAddress neuronAddress = new NeuronAddress(signal.getSourceLayerId(), signal.getSourceNeuronId());
            if (weights.containsKey(neuronAddress)) {
                for (IWeight w : weights.get(neuronAddress)) {
                    if (w.getSignalClass() == signal.getCurrentSignalClass()) {
                        return w.process(signal);
                    }
                }
                if (defaultDendritesWeights.containsKey(signal.getCurrentSignalClass())) {
                    IWeight weight = defaultDendritesWeights.get(signal.getCurrentSignalClass());
                    weights.get(neuronAddress).add(weight);
                    return weight.process(signal);
                } else {
                    logger.warn("missed weight for signal class " + signal.getCurrentSignalClass());
                    return signal;
                }
            } else {
                LinkedList<IWeight> newWeights = new LinkedList<>();
                if (defaultDendritesWeights.containsKey(signal.getCurrentSignalClass())) {
                    IWeight weight = defaultDendritesWeights.get(signal.getCurrentSignalClass());
                    newWeights.add(weight);
                    weights.put(neuronAddress, newWeights);
                    return weight.process(signal);
                } else {
                    logger.warn("missed weight for signal class " + signal.getCurrentSignalClass());
                    return signal;
                }
            }

        }).collect(Collectors.toList());
    }

    @Override
    public void removeAllWeights(NeuronAddress neuronAddress) {
        weights.remove(neuronAddress);
    }

    @Override
    public void removeWeightForClass(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass) {
        if (weights.containsKey(neuronAddress)) {
            for (IWeight w : weights.get(neuronAddress)) {
                if (w.getSignalClass() == signalClass) {
                    weights.get(neuronAddress).remove(signalClass);
                }
            }
        }
    }

    @Override
    public void moveConnection(LayerMove layerMove) {
        for (NeuronAddress neuronAddress : weights.keySet()) {
            if (neuronAddress.getLayerId().equals(layerMove.getLayerRemoved())) {
                weights.remove(neuronAddress);
            }
        }
    }
}
