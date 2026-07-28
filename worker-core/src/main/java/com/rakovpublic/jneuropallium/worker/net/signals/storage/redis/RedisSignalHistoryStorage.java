/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.neuron.NeuronAddress;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import com.rakovpublic.jneuropallium.worker.util.SignalJson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * {@code ISignalHistoryStorage} carries an interface level deserializer that expects a wrapper
 * envelope and always yields the in-memory implementation. Concrete classes inherit that
 * annotation, so it has to be cancelled here for the class to be built from its own JSON.
 */
@JsonDeserialize(using = JsonDeserializer.None.class)
public class RedisSignalHistoryStorage implements ISignalHistoryStorage {
    private final String host;
    private final Integer port;
    private final String neuronNetName;

    @JsonCreator
    public RedisSignalHistoryStorage(@JsonProperty("host") String host,
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
    public List<ISignal> getSourceSignalsForRun(Integer loop, Long nRun, NeuronAddress forTarget) {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            String stored = jedis.get(RedisKeys.history(neuronNetName, forTarget.getLayerId(), nRun, loop, forTarget.getNeuronId()));
            return new ArrayList<>(SignalJson.read(stored));
        }
    }

    @Override
    public void save(TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> history, Long run, Integer loop) {
        if (history == null || history.isEmpty()) {
            return;
        }
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            Pipeline pipeline = jedis.pipelined();
            for (Map.Entry<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> layer : history.entrySet()) {
                for (Map.Entry<Long, CopyOnWriteArrayList<ISignal>> neuron : layer.getValue().entrySet()) {
                    pipeline.set(RedisKeys.history(neuronNetName, layer.getKey(), run, loop, neuron.getKey()),
                            SignalJson.write(new ArrayList<>(neuron.getValue())));
                }
            }
            pipeline.sync();
        }
    }
}
