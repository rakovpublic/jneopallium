/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;


import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.RedisContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.JedisPool;


import java.util.HashMap;
import java.util.List;
//TODO: add implementation
public class RedisLayerMeta implements ILayerMeta {
    private static final Logger logger = LogManager.getLogger(RedisLayerMeta.class);
    private JedisPool pool= null;
    private String host;
    private Integer port;
    private String neuronNetName;

    @Override
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        return null;
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams) {

    }

    @Override
    public int getID() {
        return 0;
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {

    }

    @Override
    public List<INeuron> getNeurons() {
        return null;
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        return null;
    }

    @Override
    public void removeNeuron(Long neuron) {

    }

    @Override
    public void addNeuron(INeuron neuron) {

    }

    @Override
    public void saveNeurons(List<INeuron> neurons) {

    }

    @Override
    public void dumpLayer() {

    }

    @Override
    public Long getSize() {
        return null;
    }
}
