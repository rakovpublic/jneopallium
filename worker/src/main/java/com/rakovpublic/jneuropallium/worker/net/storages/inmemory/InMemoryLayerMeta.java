package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class InMemoryLayerMeta implements ILayerMeta {
    private List<LayerMove> layerMoves;
    private Integer id;
    private List<INeuron> neurons;

    public InMemoryLayerMeta(Integer id, List<INeuron> neurons) {
        this.id = id;
        this.neurons = neurons;
        layerMoves = new LinkedList<>();
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {
        layerMoves.add(layerMove);
    }

    @Override
    public List<? extends INeuron> getNeurons() {
        for(INeuron neuron: neurons){
            IAxon axon = neuron.getAxon();
            IDendrites dendrites = neuron.getDendrites();
            for(LayerMove layerMove :layerMoves){
                axon.moveConnection(layerMove);
                dendrites.moveConnection(layerMove);
            }
        }
        layerMoves.clear();
        return neurons;
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        for (INeuron n : neurons) {
            if (id.equals(n.getId())) {
                IAxon axon = n.getAxon();
                IDendrites dendrites = n.getDendrites();
                for(LayerMove layerMove :layerMoves){
                    axon.moveConnection(layerMove);
                    dendrites.moveConnection(layerMove);
                }
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
        return Long.parseLong(neurons.size() + "");
    }
}
