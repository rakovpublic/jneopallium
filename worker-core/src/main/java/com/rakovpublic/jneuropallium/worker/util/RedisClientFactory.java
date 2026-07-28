/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Hands out one shared {@link JedisPool} per endpoint.
 * <p>
 * Every Redis-backed storage object is re-created on each configuration update and is used
 * from several threads, so building a pool per instance - or worse, per command - exhausts
 * both the pool and the operating system socket table. Pools are keyed by {@code host:port}
 * and live for the lifetime of the JVM.
 */
public final class RedisClientFactory {
    private static final ConcurrentHashMap<String, JedisPool> POOLS = new ConcurrentHashMap<>();

    private RedisClientFactory() {
    }

    public static JedisPool pool(String host, Integer port) {
        String key = host + ":" + port;
        return POOLS.computeIfAbsent(key, k -> {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(64);
            config.setMaxIdle(16);
            config.setMinIdle(2);
            // No validation ping on borrow: connections are borrowed thousands of times per
            // partition and the extra round trip each time is pure overhead.
            config.setTestOnBorrow(false);
            return new JedisPool(config, host, port);
        });
    }

    /**
     * @return a pooled connection; always use it inside try-with-resources so it is returned.
     */
    public static Jedis jedis(String host, Integer port) {
        return pool(host, port).getResource();
    }
}
