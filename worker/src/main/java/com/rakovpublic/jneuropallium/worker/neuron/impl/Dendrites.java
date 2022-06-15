package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.neuron.IWeight;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Dendrites implements IDendrites {
    private HashMap<NeuronAddress, HashMap<Class<? extends ISignal>, IWeight>> weights;

    private HashMap<Class<? extends ISignal>, IWeight> defaultDendritesWeights;

    @Override
    public void setDefaultDendritesWeights(HashMap<Class<? extends ISignal>, IWeight> defaultDendritesWeights) {
        this.defaultDendritesWeights =defaultDendritesWeights;
    }

    @Override
    public void updateWeight(NeuronAddress neuronAddress, Class<? extends ISignal> signalClass, IWeight weight) {
        if(weights.containsKey(neuronAddress)){
            weights.get(neuronAddress).put(signalClass,weight);
        }else {
            HashMap <Class<? extends ISignal>, IWeight> weightMapping = new HashMap<>();
            weightMapping.put(signalClass,weight);
            weights.put(neuronAddress,weightMapping);
        }

    }

    @Override
    public List<ISignal> processSignalsWithDendrites(List<ISignal> signals) {
        return signals.parallelStream().map(signal -> {
            NeuronAddress neuronAddress = new NeuronAddress(signal.getSourceLayerId(),signal.getSourceNeuronId());
            if(weights.containsKey(neuronAddress)){
                if(weights.get(neuronAddress).containsKey(signal.getCurrentSignalClass())){
                    IWeight weight= weights.get(neuronAddress).get(signal.getCurrentSignalClass());
                    return weight.process(signal);

                } else    if(defaultDendritesWeights.containsKey(signal.getCurrentSignalClass())){
                    IWeight weight = defaultDendritesWeights.get(signal.getCurrentSignalClass());
                    weights.get(neuronAddress).put(signal.getCurrentSignalClass(),weight);
                    return weight.process(signal);
                }else {

                    //TODO: add warn logging of missed weight for specific signal class
                    return signal;
                }
            }else{
                HashMap<Class<? extends ISignal>, IWeight> newWeights = new HashMap<>();
                if(defaultDendritesWeights.containsKey(signal.getCurrentSignalClass())){
                    IWeight weight = defaultDendritesWeights.get(signal.getCurrentSignalClass());
                    newWeights.put(signal.getCurrentSignalClass(),weight);
                    weights.put(neuronAddress,newWeights);
                    return weight.process(signal);
                }else {
                    //TODO: add warn logging of missed weight for specific signal address
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
        if(weights.containsKey(neuronAddress)){
            if(weights.get(neuronAddress).containsKey(signalClass)){
                weights.get(neuronAddress).remove(signalClass);
            }
        }
    }

    @Override
    public void moveConnection(LayerMove layerMove) {
        for(NeuronAddress neuronAddress: weights.keySet()){
            if(neuronAddress.getLayerId().equals(layerMove.getLayerRemoved())){
                weights.remove(neuronAddress);
            }
        }
    }
}
