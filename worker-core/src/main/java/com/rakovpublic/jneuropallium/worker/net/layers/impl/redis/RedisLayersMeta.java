/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ordered set of layers of one neuron net, held in a Redis LIST of layer ids.
 * <p>
 * The list contains the regular layers in processing order and never the result layer,
 * mirroring {@code FileLayersMeta}. The cycle layer that the input loading strategy adds
 * ({@link Integer#MIN_VALUE}) is prepended so that it is processed first.
 */
public class RedisLayersMeta implements ILayersMeta {
    private static final Logger logger = LogManager.getLogger(RedisLayersMeta.class);
    private final String host;
    private final Integer port;
    private final String neuronNetName;
    /**
     * Layers created at runtime that are not backed by Redis - in practice the cycle layer the
     * input loading strategy builds. Its single control neuron is mutated in place by the master
     * on every population, so it has to stay a live object rather than a document that is parsed
     * anew on each read. Its id is still published to the shared layer list so that workers see
     * the same processing order.
     */
    private final Map<Integer, ILayerMeta> transientLayers = new ConcurrentHashMap<>();

    @JsonCreator
    public RedisLayersMeta(@JsonProperty("host") String host,
                           @JsonProperty("port") Integer port,
                           @JsonProperty("neuronNetName") String neuronNetName) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getNeuronNetName() {
        return neuronNetName;
    }

    @Override
    public void setRootPath(String path) {
        // The root of a Redis backed net is the key prefix, which is fixed at construction time.
    }

    @Override
    @JsonIgnore
    public List<ILayerMeta> getLayers() {
        List<ILayerMeta> layerMetas = new LinkedList<>();
        for (String id : layerIds()) {
            layerMetas.add(layer(Integer.parseInt(id)));
        }
        return layerMetas;
    }

    private ILayerMeta layer(int id) {
        ILayerMeta transientLayer = transientLayers.get(id);
        return transientLayer != null ? transientLayer : new RedisLayerMeta(host, port, neuronNetName, id);
    }

    @Override
    @JsonIgnore
    public IResultLayerMeta getResultLayer() {
        return new RedisResultLayerMeta(host, port, neuronNetName, RedisKeys.RESULT_LAYER_ID);
    }

    @Override
    public ILayerMeta getLayerByPosition(int position) {
        List<String> ids = layerIds();
        if (position < 0 || position >= ids.size()) {
            logger.warn("No layer at position " + position + ", the net has " + ids.size() + " layers");
            return null;
        }
        return layer(Integer.parseInt(ids.get(position)));
    }

    @Override
    public ILayerMeta getLayerById(int id) {
        if (id == RedisKeys.RESULT_LAYER_ID) {
            return getResultLayer();
        }
        if (transientLayers.containsKey(id)) {
            return transientLayers.get(id);
        }
        if (!layerIds().contains(String.valueOf(id))) {
            return null;
        }
        return new RedisLayerMeta(host, port, neuronNetName, id);
    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta) {
        addLayerMeta(layerMeta, Integer.MAX_VALUE);
    }

    /**
     * Registers a layer. {@code position} is only used to decide whether the layer goes to the
     * front of the processing order - the cycle layer uses {@link Integer#MIN_VALUE} for that.
     * Neurons carried by {@code layerMeta} are copied into Redis so that in-memory layers
     * created at runtime survive in the shared store.
     */
    @Override
    public void addLayerMeta(ILayerMeta layerMeta, int position) {
        String id = String.valueOf(layerMeta.getID());
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            if (!jedis.lrange(RedisKeys.layerIds(neuronNetName), 0, -1).contains(id)) {
                if (position == Integer.MIN_VALUE) {
                    jedis.lpush(RedisKeys.layerIds(neuronNetName), id);
                } else {
                    jedis.rpush(RedisKeys.layerIds(neuronNetName), id);
                }
            }
        }
        if (!(layerMeta instanceof RedisLayerMeta)) {
            transientLayers.put(layerMeta.getID(), layerMeta);
        }
    }

    @Override
    public void removeLayer(ILayerMeta layerMeta) {
        int id = layerMeta.getID();
        transientLayers.remove(id);
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            jedis.lrem(RedisKeys.layerIds(neuronNetName), 0, String.valueOf(id));
            jedis.del(RedisKeys.layerMeta(neuronNetName, id),
                    RedisKeys.layerNeurons(neuronNetName, id),
                    RedisKeys.layerIndex(neuronNetName, id),
                    RedisKeys.signals(neuronNetName, id));
        }
    }

    private List<String> layerIds() {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            return jedis.lrange(RedisKeys.layerIds(neuronNetName), 0, -1);
        }
    }
}
