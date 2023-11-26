/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.InputInitStrategy;

import java.util.HashMap;
import java.util.List;

public class InputInitStrategyWrapper implements InputInitStrategy {
    private InputInitStrategy iNeuronNetInput;
    private Class<? extends InputInitStrategy> clazz;

    public InputInitStrategyWrapper(InputInitStrategy iNeuronNetInput) {
        this.iNeuronNetInput = iNeuronNetInput;
        clazz = iNeuronNetInput.getClass();
    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getInputs(ILayersMeta layersMeta, List<ISignal> signals) {
        return iNeuronNetInput.getInputs(layersMeta, signals);
    }
}
