/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


import java.util.Map;

public class RedisContext implements IContext {

    private static final Logger logger = LogManager.getLogger(RedisContext.class);
    private JedisPool pool= null;
    private String host;
    private Integer port;
    private String neuronNetName;

    public RedisContext(String host, Integer port, String neuronNetName) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.pool = new JedisPool(this.host, this.port );
    }

    @Override
    public String getProperty(String propertyName) {
        if(pool==null){
            this.pool = new JedisPool(this.host, this.port );
        }
        try (Jedis jedis = pool.getResource()) {
            Map<String,String> props = jedis.hgetAll(neuronNetName+"_properties");
            return props.get(propertyName);

        }catch (Exception e){
            logger.error("Cannot extract property from redis",e);
            return null;
        }

    }

    @Override
    public void update(String path) {
        this.host = path;
        this.pool = new JedisPool(this.host);
    }
}
