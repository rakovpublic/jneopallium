/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;


import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.util.NeuronParser;
import redis.clients.jedis.JedisPooled;

import java.util.List;

public class RedisResultLayerMeta extends RedisLayerMeta implements IResultLayerMeta {

    public RedisResultLayerMeta(String host, Integer port, String neuronNetName, Integer layerId) {
        super(host, port, neuronNetName, layerId);
    }

    @Override
    public List<IResultNeuron> getResultNeurons() {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        String json = jedisPooled.jsonGet(neuronNetName+"_layer_neurons"+ layerId).toString();
        return NeuronParser.parseResultNeurons(json);
    }
}
