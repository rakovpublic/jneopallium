package net.layers.impl;

import net.layers.IResultLayer;
import net.neuron.INeuron;

import java.util.TreeMap;

public abstract class AResultLayer<K> extends Layer implements IResultLayer<K> {
    public AResultLayer(int layerId, TreeMap<INeuron,K> resultMap) {
        super(layerId);
    }

}
