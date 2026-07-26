/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.neuron.NeuronAddress;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.JedisPooled;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class RedisSignalHistoryStorage implements ISignalHistoryStorage {
    private static final Logger logger = LogManager.getLogger(RedisSignalHistoryStorage.class);
    private final String host;
    private final Integer port;
    private final String neuronNetName;

    public RedisSignalHistoryStorage(String host, Integer port, String neuronNetName) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
    }

    @Override
    public List<ISignal> getSourceSignalsForRun(Integer loop, Long nRun, NeuronAddress forTarget) {
        ObjectMapper mapper = new ObjectMapper();
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        String json = jedisPooled.jsonGet(neuronNetName + "_signalHistoryStorage_layerId_" + forTarget.getLayerId() + "_loop_" + loop + "_epoch_" + nRun + "_neuronId_" + forTarget.getNeuronId()).toString();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();

        List<ISignal> signals = new LinkedList<>();
        for (JsonElement signal : jobject.getAsJsonArray()) {
            String signalClass = signal.getAsJsonObject().getAsJsonPrimitive("currentClassName").getAsString();
            try {
                ISignal resSignal = (ISignal) mapper.readValue(signal.getAsJsonObject().toString(), Class.forName(signalClass));
                signals.add(resSignal);
            } catch (JsonProcessingException ex) {
                logger.error("Cannot parse signal for this layer " + forTarget.getLayerId() + " signal content " + signal.getAsJsonObject().toString(), ex);
                //throw new JSONParsingException("Cannot parse signal for this layer "+ layerId+" signal content "+signal.getAsJsonObject().toString() + ex.getMessage());
            } catch (ClassNotFoundException ex) {
                logger.error("Cannot find class for signal " + signalClass, ex);
                //  throw new ConfigurationClassMissedException("Cannot find class for signal "+signalClass+ ex.getMessage());
            }

        }
        return signals;
    }

    @Override
    public void save(TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> history, Long run, Integer loop) {
        ObjectMapper mapper = new ObjectMapper();
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        for (Integer layerId : history.keySet()) {
            for (Long neuronId : history.get(layerId).keySet()) {
                try {
                    jedisPooled.jsonSet(neuronNetName + "_signalHistoryStorage_layerId_" + layerId + "_loop_" + loop + "_epoch_" + run + "_neuronId_" + neuronId, mapper.writeValueAsString(history.get(layerId).get(neuronId)));
                } catch (JsonProcessingException e) {
                    logger.error("Cannot serialize to json ", e);
                }
            }
        }
    }
}
