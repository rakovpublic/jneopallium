/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;

/**
 * Runtime configuration read from Redis, so every node of the cluster is started with the same
 * three coordinates and picks up the rest - master address, thread counts - from the shared store.
 */
public class RedisContext implements IContext {

    private static final Logger logger = LogManager.getLogger(RedisContext.class);
    private String host;
    private final Integer port;
    private final String neuronNetName;

    @JsonCreator
    public RedisContext(@JsonProperty("host") String host,
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
    public String getProperty(String propertyName) {
        try (Jedis jedis = RedisClientFactory.jedis(host, port)) {
            return jedis.hget(RedisKeys.properties(neuronNetName), propertyName);
        } catch (Exception e) {
            logger.error("Cannot extract property " + propertyName + " from redis", e);
            return null;
        }
    }

    @Override
    public void update(String path) {
        this.host = path;
    }
}
