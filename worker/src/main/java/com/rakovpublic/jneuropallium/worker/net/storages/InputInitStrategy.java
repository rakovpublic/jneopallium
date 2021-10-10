package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;
@JsonDeserialize(using = InitInputDeserializer.class)
public interface InputInitStrategy {
    HashMap<Integer,HashMap<Long, List<ISignal>>> getInputs(ILayersMeta layersMeta,List<ISignal> signals);
}
