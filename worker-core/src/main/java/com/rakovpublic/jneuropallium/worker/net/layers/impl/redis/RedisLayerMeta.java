/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Layer configuration stored in Redis.
 * <p>
 * Neurons live in a HASH keyed by neuron id, with a companion ZSET that keeps the ids
 * ordered. A worker partition is a rank range over that ZSET, so fetching the neurons a
 * worker owns costs {@code ZRANGE} + {@code HMGET} instead of reading the whole layer.
 */
public class RedisLayerMeta implements ILayerMeta {
    private static final Logger logger = LogManager.getLogger(RedisLayerMeta.class);
    protected final String host;
    protected final Integer port;
    protected final String neuronNetName;
    protected final Integer layerId;

    @JsonCreator
    public RedisLayerMeta(@JsonProperty("host") String host,
                          @JsonProperty("port") Integer port,
                          @JsonProperty("neuronNetName") String neuronNetName,
                          @JsonProperty("layerId") Integer layerId) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.layerId = layerId;
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

    public Integer getLayerId() {
        return layerId;
    }

    @Override
    @JsonIgnore
    public HashMap<String, LayerMetaParam> getLayerMetaParams() {
        HashMap<String, LayerMetaParam> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            Map<String, String> stored = jedis.hgetAll(RedisKeys.layerMeta(neuronNetName, layerId));
            for (Map.Entry<String, String> entry : stored.entrySet()) {
                JsonObject json = JsonParser.parseString(entry.getValue()).getAsJsonObject();
                JsonElement paramClass = json.get("paramClass");
                JsonElement param = json.get("param");
                if (paramClass == null || param == null) {
                    continue;
                }
                try {
                    result.put(entry.getKey(), new LayerMetaParam(mapper.readValue(param.toString(), Class.forName(paramClass.getAsString()))));
                } catch (IOException | ClassNotFoundException e) {
                    logger.error("Cannot parse layer meta param " + entry.getValue(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot read layer meta params for layer " + layerId, e);
        }
        return result;
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> metaParams) {
        if (metaParams == null || metaParams.isEmpty()) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> serialized = new HashMap<>();
        for (Map.Entry<String, LayerMetaParam> entry : metaParams.entrySet()) {
            try {
                serialized.put(entry.getKey(), mapper.writeValueAsString(entry.getValue()));
            } catch (JsonProcessingException e) {
                logger.error("Cannot serialize layer meta param " + entry.getValue(), e);
            }
        }
        if (serialized.isEmpty()) {
            return;
        }
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            jedis.hset(RedisKeys.layerMeta(neuronNetName, layerId), serialized);
        }
    }

    @Override
    public int getID() {
        return layerId;
    }

    @Override
    public void addLayerMove(LayerMove layerMove) {
        List<INeuron> neurons = new LinkedList<>();
        HashMap<Long, HashMap<Integer, List<Long>>> moves = layerMove.getMovingMap();
        for (Long targetNeuronId : moves.keySet()) {
            INeuron neuron = getNeuronByID(targetNeuronId);
            if (neuron == null) {
                continue;
            }
            neuron.getAxon().moveConnection(layerMove, neuron.getLayer().getId(), targetNeuronId);
            neurons.add(neuron);
        }
        saveNeurons(neurons);
    }

    @Override
    @JsonIgnore
    public List<INeuron> getNeurons() {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            List<String> ids = jedis.zrange(RedisKeys.layerIndex(neuronNetName, layerId), 0, -1);
            return readNeurons(jedis, ids);
        }
    }

    /**
     * @param start inclusive rank of the first neuron of the partition
     * @param end   exclusive rank one past the last neuron of the partition
     */
    @Override
    public List<INeuron> getNeurons(Long start, Long end) {
        if (start == null || end == null || end <= start) {
            return new LinkedList<>();
        }
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            List<String> ids = jedis.zrange(RedisKeys.layerIndex(neuronNetName, layerId), start, end - 1);
            return readNeurons(jedis, ids);
        }
    }

    private List<INeuron> readNeurons(Jedis jedis, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedList<>();
        }
        List<String> documents = jedis.hmget(RedisKeys.layerNeurons(neuronNetName, layerId), ids.toArray(new String[0]));
        StringBuilder sb = new StringBuilder("{\"neurons\":[");
        boolean first = true;
        for (String document : documents) {
            if (document == null) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            sb.append(document);
            first = false;
        }
        sb.append("]}");
        return NeuronParser.parseNeurons(sb.toString());
    }

    @Override
    public INeuron getNeuronByID(Long id) {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            String document = jedis.hget(RedisKeys.layerNeurons(neuronNetName, layerId), String.valueOf(id));
            if (document == null) {
                return null;
            }
            return NeuronParser.parseNeuron(document);
        }
    }

    @Override
    public void removeNeuron(Long neuron) {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            jedis.hdel(RedisKeys.layerNeurons(neuronNetName, layerId), String.valueOf(neuron));
            jedis.zrem(RedisKeys.layerIndex(neuronNetName, layerId), String.valueOf(neuron));
        }
    }

    @Override
    public void addNeuron(INeuron neuron) {
        saveNeurons(List.of(neuron));
    }

    @Override
    public void saveNeurons(List<INeuron> neurons) {
        if (neurons == null || neurons.isEmpty()) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        List<String> ids = new ArrayList<>(neurons.size());
        List<String> documents = new ArrayList<>(neurons.size());
        for (INeuron neuron : neurons) {
            try {
                documents.add(mapper.writeValueAsString(neuron));
                ids.add(String.valueOf(neuron.getId()));
            } catch (JsonProcessingException e) {
                logger.error("Cannot serialize neuron " + neuron.getId(), e);
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            Pipeline pipeline = jedis.pipelined();
            for (int i = 0; i < ids.size(); i++) {
                pipeline.hset(RedisKeys.layerNeurons(neuronNetName, layerId), ids.get(i), documents.get(i));
                pipeline.zadd(RedisKeys.layerIndex(neuronNetName, layerId), Double.parseDouble(ids.get(i)), ids.get(i));
            }
            pipeline.sync();
        }
    }

    @Override
    public void dumpLayer() {
        // Neurons are written straight through on save; nothing is buffered in memory.
    }

    @Override
    @JsonIgnore
    public Long getSize() {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            return jedis.zcard(RedisKeys.layerIndex(neuronNetName, layerId));
        }
    }
}
