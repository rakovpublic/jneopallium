package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface InputInitStrategy {
    List<HashMap<Long, List<ISignal>>> getInputs(ILayersMeta layersMeta,List<ISignal> signals);
}
