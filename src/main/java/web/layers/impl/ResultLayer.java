package web.layers.impl;

import web.layers.IResultLayer;
import web.neuron.INeuron;

import java.util.HashMap;
import java.util.TreeMap;

public class ResultLayer<K> extends Layer implements IResultLayer<K> {
    public ResultLayer(int layerId, TreeMap<INeuron,K> resultMap) {
        super(layerId);
    }

    @Override
    public K interpretResult() {
        return null;
    }
}
