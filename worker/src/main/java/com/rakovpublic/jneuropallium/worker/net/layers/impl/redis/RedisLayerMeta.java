/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMetaParam;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.util.NeuronParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path2;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RedisLayerMeta implements ILayerMeta {
    private static final Logger logger = LogManager.getLogger(RedisLayerMeta.class);
    protected JedisPool pool = null;
    protected String host;
    protected Integer port;
    protected String neuronNetName;
    protected Integer layerId;

    public RedisLayerMeta(String host, Integer port, String neuronNetName, Integer layerId) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.layerId = layerId;
        this.pool = new JedisPool(this.host, this.port);
    }

    @Override
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        HashMap<String, LayerMetaParam> result = new HashMap<>();
        if (pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> jmetaParams = jedis.hgetAll(neuronNetName + "_layer_metaparam" + layerId);
            for (String paramName : jmetaParams.keySet()) {
                JsonElement jelement = new JsonParser().parse(jmetaParams.get(paramName));
                JsonObject jobject = jelement.getAsJsonObject();
                String cl = jobject.getAsJsonPrimitive("paramClass").getAsString();
                ObjectMapper mapper = new ObjectMapper();
                LayerMetaParam metaParam = null;
                try {
                    metaParam = new LayerMetaParam(mapper.readValue(jobject.getAsJsonPrimitive("param").getAsString(), Class.forName(cl)));
                } catch (IOException | ClassNotFoundException e) {
                    logger.error("cannot parse layer meta param from json " + jmetaParams.get(paramName), e);
                }
                if (metaParam != null) {
                    result.put(paramName, metaParam);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot extract property from redis", e);
            return null;
        }
        return result;
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams) {
        if (pool == null) {
            this.pool = new JedisPool(this.host, this.port);
        }
        HashMap<String, String> tmap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (String paramName: metaParams.keySet()){
            try {
                tmap.put(paramName,mapper.writeValueAsString(metaParams.get(paramName)));
            } catch (JsonProcessingException e) {
                logger.error("cannot put layer meta param to json " + metaParams.get(paramName), e);
            }
        }
        pool.getResource().hset(neuronNetName + "_layer_metaparam" + layerId,tmap);
    }

    @Override
    public int getID() {
        return layerId;
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {
        List<INeuron>  neurons = new LinkedList<>();
        HashMap<Long, HashMap<Integer,List<Long>>> moves = layerMove.getMovingMap();
        for(Long targetNeuronId:moves.keySet()){
            INeuron neuron = getNeuronByID(targetNeuronId);
            neuron.getAxon().moveConnection(layerMove,neuron.getLayer().getId(),targetNeuronId);
            neurons.add(neuron);
        }
        saveNeurons(neurons);
    }

    @Override
    public List<INeuron> getNeurons() {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        String json = jedisPooled.jsonGet(neuronNetName+"_layer_neurons"+ layerId).toString();
        return NeuronParser.parseNeurons(json);
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        String json = jedisPooled.jsonGet(neuronNetName+"_layer_neurons"+ layerId, Path2.of("$..[?(@.neuronId == "+id+" )]")).toString();
        return NeuronParser.parseNeuron(json);
    }

    @Override
    public void removeNeuron(Long neuron) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        jedisPooled.jsonDel(neuronNetName+"_layer_neurons"+ layerId, Path2.of("$..[?(@.neuronId == "+neuron+" )]"));
    }

    @Override
    public void addNeuron(INeuron neuron) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        ObjectMapper mapper = new ObjectMapper();
        try {
            jedisPooled.jsonMerge(neuronNetName+"_layer_neurons"+ layerId,Path2.of("$"),mapper.writeValueAsString(neuron));
        } catch (JsonProcessingException e) {
            logger.error("Cannot add neuron", e);
        }

    }

    @Override
    public void saveNeurons(List<INeuron> neurons) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for(INeuron neuron: neurons){
            sb.append(neuron.getId()+",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("]");
        jedisPooled.jsonDel(neuronNetName+"_layer_neurons"+ layerId, Path2.of("$..[?(@.neuronId in "+sb.toString()+" )]"));
        for (INeuron neuron: neurons){
            addNeuron(neuron);
        }
    }

    @Override
    public void dumpLayer() {

    }

    @Override
    public Long getSize() {
        return Long.parseLong( getNeurons().size()+"");
    }
}
