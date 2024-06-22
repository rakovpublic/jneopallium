/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.args.ListPosition;

import java.util.LinkedList;
import java.util.List;

public class RedisLayersMeta implements ILayersMeta {
    private static final Logger logger = LogManager.getLogger(RedisLayersMeta.class);
    private JedisPool pool = null;
    private final String host;
    private final Integer port;
    private final String neuronNetName;

    public RedisLayersMeta(String host, Integer port, String neuronNetName) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.pool = new JedisPool(this.host, this.port);
    }

    @Override
    public void setRootPath(String path) {

    }

    @Override
    public List<ILayerMeta> getLayers() {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        List<ILayerMeta> layerMetas = new LinkedList<>();
        List<String> ids = pool.getResource().lrange(neuronNetName + "_layerIds", 0, pool.getResource().llen(neuronNetName + "_layerIds"));
        for (String id : ids) {
            layerMetas.add(new RedisLayerMeta(host, port, neuronNetName, Integer.parseInt(id)));
        }

        return layerMetas;
    }

    @Override
    public IResultLayerMeta getResultLayer() {

        return new RedisResultLayerMeta(host, port, neuronNetName, Integer.MAX_VALUE);
    }

    @Override
    public ILayerMeta getLayerByPosition(int id) {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        String idR = pool.getResource().lindex(neuronNetName + "_layerIds", Long.parseLong(id + ""));
        return new RedisLayerMeta(host, port, neuronNetName, Integer.parseInt(idR));
    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta) {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        pool.getResource().rpush(neuronNetName + "_layerIds", layerMeta.getID() + "");
    }

    @Override
    public void addLayerMeta(ILayerMeta layerMeta, int position) {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        pool.getResource().linsert(neuronNetName + "_layerIds", ListPosition.BEFORE, getLayerByPosition(position) + "", layerMeta.getID() + "");
    }

    @Override
    public void removeLayer(ILayerMeta layerMeta) {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        pool.getResource().lrem(neuronNetName + "_layerIds", 0, layerMeta.getID() + "");
        for (String name : layerMeta.getLayerMetaParams().keySet()) {
            pool.getResource().hdel(neuronNetName + "_layer_metaparam" + layerMeta.getID(), name);
        }
        pool.getResource().hdel(neuronNetName + "_layer_metaparam" + layerMeta.getID(), "");
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        jedisPooled.jsonDel(neuronNetName + "_layer_neurons" + layerMeta.getID());
        jedisPooled.jsonDel(neuronNetName + "_layer_neurons" + layerMeta.getID());
    }

    @Override
    public ILayerMeta getLayerById(int id) {
        if (this.pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        List<ILayerMeta> layerMetas = new LinkedList<>();
        List<String> ids = pool.getResource().lrange(neuronNetName + "_layerIds", 0, pool.getResource().llen(neuronNetName + "_layerIds"));
        return getLayerByPosition(ids.indexOf(id + ""));
    }
}
