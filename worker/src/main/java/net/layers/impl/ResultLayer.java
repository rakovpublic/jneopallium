package net.layers.impl;

import net.layers.IResultLayer;
import net.neuron.INeuron;

import java.util.TreeMap;

public class ResultLayer<K> extends Layer implements IResultLayer<K> {
    private TreeMap<INeuron, K> resultMap;

    public ResultLayer(int layerId, TreeMap<INeuron, K> resultMap) {
        super(layerId);
        this.resultMap = resultMap;
    }

    @Override
    public K interpretResult() {
        if (this.isProcessed()) {
            return resultMap.get(resultMap.lastKey());
        }
        return null;
    }
}
