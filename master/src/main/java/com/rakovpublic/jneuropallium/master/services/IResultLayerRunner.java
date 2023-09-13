package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.HashMap;
import java.util.List;

public interface IResultLayerRunner {
    List<? extends IResultNeuron> getResults(IResultLayerMeta resultLayer, HashMap<Long, List<ISignal>> signals);

}
