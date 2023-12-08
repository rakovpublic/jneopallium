package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class ResultLayer<K extends IResultSignal> extends Layer implements IResultLayer {

    public ResultLayer(int layerId, IInputResolver meta, int threads) {
        super(layerId, meta, threads);

    }

    @Override
    public List<IResult> interpretResult() {
        TreeSet<INeuron> resultNeurons = new TreeSet<>();
        List<IResult> res = new ArrayList<>();
        while (!this.isProcessed()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (this.isProcessed()) {
            resultNeurons.addAll(this.map.values());
            res.add(new SimpleResultWrapper(((IResultNeuron) resultNeurons.last()).getFinalResult(), resultNeurons.last().getId()));
            return res;
        }
        return null;
    }
}
