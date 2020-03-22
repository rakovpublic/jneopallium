package net.layers.impl;

import net.layers.IResult;
import net.layers.IResultLayer;
import net.neuron.INeuron;
import net.storages.IInputMeta;

import java.util.TreeMap;

public class ResultLayer<K> extends Layer implements IResultLayer<K> {
    private TreeMap<INeuron, K> resultMap;

    public ResultLayer(int layerId, TreeMap<INeuron, K> resultMap, IInputMeta meta) {
        super(layerId,meta);
        this.resultMap = resultMap;
    }

    @Override
    public IResult<K> interpretResult() {
        if (this.isProcessed()) {
            return new SimpleResultWrapper<K>(resultMap.get(resultMap.lastKey()), resultMap.lastKey().getId());
        }
        return null;
    }
}
