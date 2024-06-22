/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.*;

public class KafkaInitInput implements IInitInput {
    private static final Logger logger = LogManager.getLogger(KafkaInitInput.class);
    private final String name;
    private final String host;
    private final String topic;
    private final ProcessingFrequency defaultProcessingFrequency;

    public KafkaInitInput(String name, String host, String topic, ProcessingFrequency defaultProcessingFrequency) {
        this.name = name;
        this.host = host;
        this.topic = topic;
        this.defaultProcessingFrequency = defaultProcessingFrequency;
    }

    @Override
    public List<IInputSignal> readSignals() {

        Properties consumerProperties = new Properties();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, name);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties);
        consumer.subscribe(Arrays.asList(topic));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1000));
        List<IInputSignal> result = new LinkedList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (ConsumerRecord<String, String> record : records) {
            JsonElement jelement = new JsonParser().parse(record.value());
            JsonObject jobject = jelement.getAsJsonObject();
            String signalClass = jobject.getAsJsonPrimitive("currentClassName").getAsString();
            IInputSignal resSignal = null;
            try {
                resSignal = (IInputSignal) mapper.readValue(jobject.getAsJsonObject().toString(), Class.forName(signalClass));
            } catch (JsonProcessingException e) {
                logger.error("Cannot parse signal  " + jobject.getAsJsonObject().toString(), e);
            } catch (ClassNotFoundException e) {
                logger.error("Cannot find class for signal " + signalClass, e);
            }
            result.add(resSignal);


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
