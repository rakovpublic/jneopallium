package net.layers.impl;

import net.layers.IResult;
import net.layers.IResultLayer;
import net.neuron.INeuron;
import net.neuron.IResultNeuron;
import net.signals.IResultSignal;
import net.storages.IInputMeta;

import java.util.TreeSet;

public class ResultLayer<K> extends Layer implements IResultLayer<K> {

    public ResultLayer(int layerId, IInputMeta meta) {
        super(layerId,meta);

    }

    @Override
    public IResult<K> interpretResult() {
        TreeSet<INeuron> resultNeurons= new TreeSet<>();
        if (this.isProcessed()) {
            resultNeurons.addAll(this.map.values());
            return new SimpleResultWrapper<K>(((IResultNeuron)resultNeurons.last()).getFinalResult(),resultNeurons.last().getId());
        }
        return null;
    }
}
