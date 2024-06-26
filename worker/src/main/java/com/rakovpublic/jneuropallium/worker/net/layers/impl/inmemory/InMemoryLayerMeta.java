/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.inmemory;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.IDendrites;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryLayerMeta implements ILayerMeta {
    private final List<LayerMove> layerMoves;
    private final Integer id;
    private List<INeuron> neurons;
    private HashMap<String, LayerMetaParam> layerMetaParams;

    public InMemoryLayerMeta(Integer id, List<INeuron> neurons, HashMap<String, LayerMetaParam> metaParams) {
        this.id = id;
        this.neurons = neurons;
        layerMetaParams = metaParams;
        layerMoves = new LinkedList<>();
    }


    @Override
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        return layerMetaParams;
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams) {
        this.layerMetaParams = metaParams;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {
        HashMap<Long, HashMap<Integer, List<Long>>> moves = layerMove.getMovingMap();
        for (Long targetNeuronId : moves.keySet()) {
            INeuron neuron = getNeuronByID(targetNeuronId);
            neuron.getAxon().moveConnection(layerMove, id, targetNeuronId);
        }
    }

    @Override
    public List<INeuron> getNeurons() {
        for (INeuron neuron : neurons) {
            IAxon axon = neuron.getAxon();
            IDendrites dendrites = neuron.getDendrites();
            for (LayerMove layerMove : layerMoves) {
                if (layerMove.getMovingMap().containsKey(neuron.getId())) {
                    axon.moveConnection(layerMove, this.id, neuron.getId());
                }
                dendrites.moveConnection(layerMove);
            }
        }
        layerMoves.clear();
        return neurons;
    }

    @Override
    public List<INeuron> getNeurons(Long start, Long end) {
        List<INeuron> result = new LinkedList<>();
        for (INeuron neuron : neurons) {
            if (neuron.getId() >= start && neuron.getId() < end) {
                result.add(neuron);
            }
        }
        return result;
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        for (INeuron n : neurons) {
            if (id.equals(n.getId())) {
                IAxon axon = n.getAxon();
                IDendrites dendrites = n.getDendrites();
                for (LayerMove layerMove : layerMoves) {
                    if (layerMove.getMovingMap().containsKey(n.getId())) {
                        axon.moveConnection(layerMove, this.id, n.getId());
                    }
                    dendrites.moveConnection(layerMove);
                }
                return n;
            }
        }
        return null;
    }

    @Override
    public void removeNeuron(Long neuron) {
        for (INeuron n : neurons) {
            if (n.getId() == neuron) {
                neurons.remove(n);
            }
        }
    }

    @Override
    public void addNeuron(INeuron neuron) {
        neurons.add(neuron);
    }

    @Override
    public void saveNeurons(List<INeuron> neuronMetas) {
        neurons = neuronMetas;
    }


    @Override
    public void dumpLayer() {

    }

    @Override
    public Long getSize() {
        return Long.parseLong(neurons.size() + "");
    }
}
