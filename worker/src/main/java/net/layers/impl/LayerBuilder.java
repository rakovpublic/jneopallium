package net.layers.impl;

import net.layers.ILayer;
import net.neuron.INeuron;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;
import net.storages.ILayersMeta;
import net.storages.INeuronMeta;

import java.util.HashMap;
import java.util.List;


public class LayerBuilder {
    private ILayerMeta layerMeta;
    private IInputMeta meta;

    public LayerBuilder() {

    }

    public LayerBuilder withLayer(ILayerMeta layerMeta){
        this.layerMeta=layerMeta;
        return this;
    }
    public LayerBuilder withInput(IInputMeta meta){
        this.meta=meta;
        return this;
    }
    public LayerBuilder withNeuronRange(int start, int end){

        //TODO:add implementation
        return  this;
    }

    public ILayer build(){
        ILayer layer= new Layer(layerMeta.getID());
        for(INeuronMeta ner:layerMeta.getNeurons()){
            layer.register(ner.toNeuron());
        }
        HashMap<Long, List<ISignal>> inputs= meta.readInputs(layerMeta.getID());
        for(Long neuronID:inputs.keySet()){
            for(ISignal signal: inputs.get(neuronID)){
                layer.addInput(signal,neuronID);
            }
        }
        return layer;
    }

}
