package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.storages.IInputMeta;

import java.util.TreeSet;

public class ResultLayer<K> extends Layer implements IResultLayer<K> {

    public ResultLayer(int layerId, IInputResolver meta) {
        super(layerId,meta);

    }

    @Override
    public IResult<K> interpretResult() {
        TreeSet<INeuron> resultNeurons= new TreeSet<>();
        while (!this.isProcessed()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (this.isProcessed()) {
            resultNeurons.addAll(this.map.values());
            return new SimpleResultWrapper(((IResultNeuron)resultNeurons.last()).getFinalResult(),resultNeurons.last().getId());
        }
        return null;
    }
}
