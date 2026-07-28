/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.util.RedisClientFactory;
import com.rakovpublic.jneuropallium.worker.util.RedisKeys;
import com.rakovpublic.jneuropallium.worker.util.SignalJson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Input source backed by a Redis list.
 * <p>
 * Producers push batches with {@code RPUSH <net>_input_<name> '[{signal},...]'}; every read
 * drains the list, so each batch is consumed exactly once. Being a list rather than a shared
 * key also means an operator can inject signals during the demo with plain {@code redis-cli}.
 */
public class RedisInitInput implements IInitInput {
    private static final Logger logger = LogManager.getLogger(RedisInitInput.class);
    private final String host;
    private final Integer port;
    private final String neuronNetName;
    private final String name;
    private final ProcessingFrequency defaultProcessingFrequency;

    @JsonCreator
    public RedisInitInput(@JsonProperty("host") String host,
                          @JsonProperty("port") Integer port,
                          @JsonProperty("neuronNetName") String neuronNetName,
                          @JsonProperty("name") String name,
                          @JsonProperty("defaultProcessingFrequency") ProcessingFrequency defaultProcessingFrequency) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.name = name;
        this.defaultProcessingFrequency = defaultProcessingFrequency == null
                ? new ProcessingFrequency(1L, 1) : defaultProcessingFrequency;
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
    public List<IInputSignal> readSignals() {
        List<IInputSignal> result = new LinkedList<>();
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            String key = RedisKeys.inputQueue(neuronNetName, name);
            // One list element is one batch, and one batch is what the net consumes per read,
            // so pushing N batches feeds the net for N populations.
            String batch = jedis.lpop(key);
            if (batch == null) {
                return result;
            }
            for (ISignal signal : SignalJson.read(batch)) {
                if (signal instanceof IInputSignal) {
                    result.add((IInputSignal) signal);
                } else {
                    logger.error("Input queue " + key + " holds a non input signal: " + signal.getClass());
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return defaultProcessingFrequency;
    }
}
