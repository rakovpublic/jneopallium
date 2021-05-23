package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.Collection;
import java.util.List;

public class InMemoryLayerMeta implements ILayerMeta {
    private Integer id;
    private List<INeuron> neurons;

    public InMemoryLayerMeta(Integer id, List<INeuron> neurons) {
        this.id = id;
        this.neurons = neurons;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        return neurons;
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        for(INeuron n:neurons){
            if(id.equals(n.getId())){
                return n;
            }
        }
        return null;
    }

    @Override
    public void saveNeurons(Collection<? extends INeuron> neuronMetas) {

    }

    @Override
    public void dumpLayer() {

    }

    @Override
    public Long getSize() {
        return Long.parseLong(neurons.size()+"");
    }
}
