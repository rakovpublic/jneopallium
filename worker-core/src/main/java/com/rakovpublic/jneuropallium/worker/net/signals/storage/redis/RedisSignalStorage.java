/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import com.rakovpublic.jneuropallium.worker.util.SignalJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pending signals of the whole net, shared by every worker.
 * <p>
 * One HASH per layer, field per neuron. Workers only ever touch the fields of the neurons
 * in their own partition, so two workers writing different partitions of the same layer do
 * not overwrite each other.
 */
public class RedisSignalStorage implements ISignalsPersistStorage {
    private static final Logger logger = LogManager.getLogger(RedisSignalStorage.class);
    private final String host;
    private final Integer port;
    private final String neuronNetName;

    @JsonCreator
    public RedisSignalStorage(@JsonProperty("host") String host,
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
    public void putSignals(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            for (Map.Entry<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> layer : signals.entrySet()) {
                String key = RedisKeys.signals(neuronNetName, layer.getKey());
                List<String> fields = new ArrayList<>();
                for (Map.Entry<Long, CopyOnWriteArrayList<ISignal>> neuron : layer.getValue().entrySet()) {
                    if (neuron.getValue() != null && !neuron.getValue().isEmpty()) {
                        fields.add(String.valueOf(neuron.getKey()));
                    }
                }
                if (fields.isEmpty()) {
                    continue;
                }
                // Read every affected neuron in one round trip and write them back in one pipeline;
                // a signal per round trip is what makes a large partition crawl.
                List<String> stored = jedis.hmget(key, fields.toArray(new String[0]));
                Map<String, String> updated = new HashMap<>();
                for (int i = 0; i < fields.size(); i++) {
                    List<ISignal> merged = new ArrayList<>(SignalJson.read(stored.get(i)));
                    merged.addAll(layer.getValue().get(Long.parseLong(fields.get(i))));
                    updated.put(fields.get(i), SignalJson.write(merged));
                }
                jedis.hset(key, updated);
            }
        }
    }

    @Override
    public HashMap<Long, CopyOnWriteArrayList<ISignal>> getLayerSignals(Integer layerId) {
        HashMap<Long, CopyOnWriteArrayList<ISignal>> result = new HashMap<>();
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            Map<String, String> stored = jedis.hgetAll(RedisKeys.signals(neuronNetName, layerId));
            for (Map.Entry<String, String> entry : stored.entrySet()) {
                result.put(Long.parseLong(entry.getKey()), SignalJson.read(entry.getValue()));
            }
        } catch (NumberFormatException e) {
            logger.error("Corrupted signal key in layer " + layerId, e);
        }
        return result;
    }

    @Override
    public void cleanOutdatedSignals() {
        TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals = getAllSignals();
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            for (Map.Entry<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> layer : signals.entrySet()) {
                String key = RedisKeys.signals(neuronNetName, layer.getKey());
                jedis.del(key);
                for (Map.Entry<Long, CopyOnWriteArrayList<ISignal>> neuron : layer.getValue().entrySet()) {
                    List<ISignal> survivors = new ArrayList<>();
                    for (ISignal signal : neuron.getValue()) {
                        ISignal next = signal == null ? null : signal.prepareSignalToNextStep();
                        if (next != null) {
                            survivors.add(next);
                        }
                    }
                    if (!survivors.isEmpty()) {
                        jedis.hset(key, String.valueOf(neuron.getKey()), SignalJson.write(survivors));
                    }
                }
            }
        }
    }

    @Override
    public void cleanMiddleLayerSignals() {
        TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals = getAllSignals();
        if (signals.size() <= 2) {
            return;
        }
        List<Integer> middle = new ArrayList<>(signals.keySet());
        middle.remove(middle.size() - 1);
        middle.remove(0);
        for (Integer layer : middle) {
            deletedLayerInput(layer);
        }
    }

    @Override
    public TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getAllSignals() {
        TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new TreeMap<>();
        for (Integer layerId : layerIds()) {
            HashMap<Long, CopyOnWriteArrayList<ISignal>> layerSignals = getLayerSignals(layerId);
            if (!layerSignals.isEmpty()) {
                result.put(layerId, layerSignals);
            }
        }
        return result;
    }

    @Override
    public void deletedLayerInput(Integer deletedLayerId) {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            jedis.del(RedisKeys.signals(neuronNetName, deletedLayerId));
        }
    }

    @Override
    public boolean hasSignalsToProcess() {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            for (Integer layerId : layerIds()) {
                if (jedis.hlen(RedisKeys.signals(neuronNetName, layerId)) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return every layer that can hold signals - the registered layers plus the result layer.
     */
    private List<Integer> layerIds() {
        List<Integer> ids = new ArrayList<>();
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            for (String id : jedis.lrange(RedisKeys.layerIds(neuronNetName), 0, -1)) {
                ids.add(Integer.parseInt(id));
            }
        } catch (NumberFormatException e) {
            logger.error("Corrupted layer id list for net " + neuronNetName, e);
        }
        ids.add(RedisKeys.RESULT_LAYER_ID);
        return ids;
    }
}
