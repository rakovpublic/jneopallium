/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;

import java.util.ArrayList;
import java.util.List;

public class RedisResultLayerMeta extends RedisLayerMeta implements IResultLayerMeta {

    @JsonCreator
    public RedisResultLayerMeta(@JsonProperty("host") String host,
                                @JsonProperty("port") Integer port,
                                @JsonProperty("neuronNetName") String neuronNetName,
                                @JsonProperty("layerId") Integer layerId) {
        super(host, port, neuronNetName, layerId);
    }

    @Override
    @JsonIgnore
    public List<IResultNeuron> getResultNeurons() {
        List<IResultNeuron> result = new ArrayList<>();
        for (INeuron neuron : getNeurons()) {
            if (neuron instanceof IResultNeuron) {
                result.add((IResultNeuron) neuron);
            }
        }
        return result;
    }
}
