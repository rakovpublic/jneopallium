package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ResultLayer<K extends IResultSignal> extends Layer implements IResultLayer {

    public ResultLayer(int layerId, IInputResolver meta, int threads) {
        super(layerId, meta, threads);

    }

    @Override
    public List<IResult> interpretResult() {
        Set<IResultNeuron> resultNeurons = new HashSet<>();
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
            for (IResultNeuron resultNeuron : resultNeurons) {
                res.add(new SimpleResultWrapper(resultNeuron.getFinalResult(), resultNeuron.getId()));
            }

            return res;
        }
        return null;
    }
}
