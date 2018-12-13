package web.layers.impl;

import web.layers.IResultLayer;
import web.neuron.INeuron;

import java.util.HashMap;
import java.util.TreeMap;

public abstract class AResultLayer<K> extends Layer implements IResultLayer<K> {
    public AResultLayer(int layerId, TreeMap<INeuron,K> resultMap) {
        super(layerId);
    }

}
