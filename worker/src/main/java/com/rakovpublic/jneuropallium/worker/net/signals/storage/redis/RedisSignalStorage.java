/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.exceptions.ConfigurationClassMissedException;
import com.rakovpublic.jneuropallium.worker.exceptions.JSONParsingException;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.redis.RedisLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalsPersistStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

import java.util.*;

public class RedisSignalStorage implements ISignalsPersistStorage {
    private static final Logger logger = LogManager.getLogger(RedisSignalStorage.class);
    private JedisPool pool = null;
    private String host;
    private Integer port;
    private String neuronNetName;

    public RedisSignalStorage(String host, Integer port, String neuronNetName) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.pool = new JedisPool(this.host, this.port);
    }

    @Override
    public void putSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        ObjectMapper mapper = new ObjectMapper();
        for(Integer layerId:signals.keySet()){
            HashMap<Long, List<ISignal>> existedSignals= getLayerSignals(layerId);
            HashMap<Long, List<ISignal>> newSignal = signals.get(layerId);
            for (Long neuron: newSignal.keySet()){
                if(existedSignals!= null&&existedSignals.containsKey(neuron)){
                    existedSignals.get(neuron).addAll(newSignal.get(neuron));
                }else if(existedSignals==null){
                    existedSignals= new HashMap<>();
                    existedSignals.put(neuron,newSignal.get(neuron));
                }else {
                    existedSignals.put(neuron,newSignal.get(neuron));
                }
            }

            try {
                jedisPooled.jsonSet(neuronNetName+"_signalStorage_layerId_"+layerId,mapper.writeValueAsString(existedSignals));
            } catch (JsonProcessingException e) {
                logger.error("Cannot save signals for this layer "+ layerId+" signals content "+existedSignals.toString(), e);
            }
        }

    }

    @Override
    public HashMap<Long, List<ISignal>> getLayerSignals(Integer layerId) {
        HashMap<Long, List<ISignal>> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        String json = jedisPooled.jsonGet(neuronNetName+"_signalStorage_layerId_"+layerId).toString();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> e : jobject.entrySet()){
            Long id= Long.parseLong(e.getKey());
            List<ISignal> signals = new LinkedList<>();
            for(JsonElement signal: e.getValue().getAsJsonArray()){
               String signalClass = signal.getAsJsonObject().getAsJsonPrimitive("currentClassName").getAsString();
                try {
                    ISignal resSignal = (ISignal)mapper.readValue(signal.getAsJsonObject().toString(),Class.forName(signalClass));
                    signals.add(resSignal);
                } catch (JsonProcessingException ex) {
                    logger.error("Cannot parse signal for this layer "+ layerId+" signal content "+signal.getAsJsonObject().toString(), ex);
                    //throw new JSONParsingException("Cannot parse signal for this layer "+ layerId+" signal content "+signal.getAsJsonObject().toString() + ex.getMessage());
                } catch (ClassNotFoundException ex) {
                    logger.error("Cannot find class for signal "+signalClass, ex);
                  //  throw new ConfigurationClassMissedException("Cannot find class for signal "+signalClass+ ex.getMessage());
                }
            }
            result.put(id,signals);

        }
        return result;
    }

    @Override
    public void cleanOutdatedSignals() {
        TreeMap<Integer, HashMap<Long, List<ISignal>>> signals = getAllSignals();
        HashMap<Integer, HashMap<Long, List<ISignal>>> toSet= new HashMap<>();
        for(Integer layerId: signals.keySet()){
            HashMap<Long, List<ISignal>> layerSignals= signals.get(layerId);
            for(Long neuronId: layerSignals.keySet()){
                List<ISignal> newSignals = new LinkedList<>();
                for(ISignal signal:layerSignals.get(neuronId)){
                    ISignal signalNew =signal.prepareSignalToNextStep();
                    if(signalNew!=null){
                        newSignals.add(signalNew);
                    }
                }
                layerSignals.put(neuronId,newSignals);
            }
            deletedLayerInput(layerId);
           toSet.put(layerId,layerSignals);
        }
        putSignals(toSet);
    }

    @Override
    public void cleanMiddleLayerSignals() {
        TreeMap<Integer, HashMap<Long, List<ISignal>>> signals = getAllSignals();
        signals.remove(signals.firstKey());
        signals.remove(signals.firstKey());
        signals.remove(signals.lastKey());
        for(Integer layer: signals.keySet()){
            deletedLayerInput(layer);
        }


    }

    @Override
    public TreeMap<Integer, HashMap<Long, List<ISignal>>> getAllSignals() {
        TreeMap<Integer, HashMap<Long, List<ISignal>>> result = new TreeMap<>();
        if(this.pool==null){
            this.pool = new JedisPool(this.host, this.port);
        }
        List<String> ids = pool.getResource().lrange(neuronNetName+"_layerIds",0, pool.getResource().llen(neuronNetName+"_layerIds"));
        for(String id: ids){
            Integer layerId= Integer.parseInt(id);
            result.put(layerId,getLayerSignals(layerId));
        }
        return result;
    }

    @Override
    public void deletedLayerInput(Integer deletedLayerId) {
        JedisPooled jedisPooled = new JedisPooled(this.host, this.port);
        jedisPooled.jsonDel(neuronNetName+"_signalStorage_layerId_"+deletedLayerId);
    }
}
