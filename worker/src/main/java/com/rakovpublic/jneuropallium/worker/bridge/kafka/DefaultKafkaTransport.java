/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production {@link KafkaTransport} backed by {@link KafkaConsumer} and
 * {@link KafkaProducer} from the Apache Kafka client library
 * (08-KAFKA.md §3, §5).
 *
 * <p>One {@code KafkaConsumer} is created per binding (per-binding consumer
 * groups isolate offset tracking; mass rebalance of one binding's topic
 * cannot stall another). One shared {@code KafkaProducer} handles all
 * advisory writes.
 *
 * <p>Tests do <b>not</b> use this class — they inject an in-memory
 * {@link KafkaTransport} (see {@code InMemoryKafkaTransport} in the test
 * tree). This class is wired by deployment code that has live brokers.
 */
public final class DefaultKafkaTransport implements KafkaTransport {

    private static final Logger log = LoggerFactory.getLogger(DefaultKafkaTransport.class);

    private final KafkaBridgeConfig config;
    private final Map<String, KafkaConsumer<String, byte[]>> consumers = new ConcurrentHashMap<>();
    private final KafkaProducer<String, byte[]> producer;

    public DefaultKafkaTransport(KafkaBridgeConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.producer = new KafkaProducer<>(producerProps(config));
    }

    @Override
    public synchronized void subscribe(String bindingId, String topic, String groupId) {
        if (consumers.containsKey(bindingId)) return;
        Properties p = consumerProps(config, groupId);
        KafkaConsumer<String, byte[]> c = new KafkaConsumer<>(p);
        c.subscribe(java.util.List.of(topic));
        consumers.put(bindingId, c);
        log.info("Subscribed binding={} topic={} group={}", bindingId, topic, groupId);
    }

    @Override
    public List<InboundRecord> poll(String bindingId, Duration timeout, int maxRecords) {
        KafkaConsumer<String, byte[]> c = consumers.get(bindingId);
        if (c == null) {
            throw new KafkaTransportException("Unknown binding " + bindingId);
        }
        ConsumerRecords<String, byte[]> recs;
        try {
            recs = c.poll(timeout);
        } catch (Exception e) {
            throw new KafkaTransportException("poll failed for " + bindingId, e);
        }
        List<InboundRecord> out = new ArrayList<>(Math.min(recs.count(), maxRecords));
        int n = 0;
        for (ConsumerRecord<String, byte[]> rec : recs) {
            if (n++ >= maxRecords) break;
            out.add(new InboundRecord(rec.topic(), rec.partition(), rec.offset(),
                    rec.key(), rec.value()));
        }
        return out;
    }

    @Override
    public void commitSync(String bindingId, Map<Integer, Long> partitionToOffset) {
        KafkaConsumer<String, byte[]> c = consumers.get(bindingId);
        if (c == null) return;
        Map<TopicPartition, OffsetAndMetadata> ofs = new HashMap<>();
        for (Map.Entry<Integer, Long> e : partitionToOffset.entrySet()) {
            // The consumer is subscribed to exactly one topic per binding (per the
            // subscribe() contract above), so the topic is unambiguous here.
            for (TopicPartition tp : c.assignment()) {
                if (tp.partition() == e.getKey()) {
                    ofs.put(tp, new OffsetAndMetadata(e.getValue()));
                }
            }
        }
        if (!ofs.isEmpty()) {
            try { c.commitSync(ofs); }
            catch (Exception ex) { throw new KafkaTransportException("commitSync failed", ex); }
        }
    }

    @Override
    public void send(String topic, String key, byte[] value) {
        try {
            producer.send(new ProducerRecord<>(topic, key, value)).get();
        } catch (Exception e) {
            throw new KafkaTransportException("send to topic=" + topic + " failed", e);
        }
    }

    @Override
    public synchronized void close() {
        for (KafkaConsumer<String, byte[]> c : consumers.values()) {
            try { c.close(Duration.ofSeconds(2)); } catch (RuntimeException e) {
                log.warn("Consumer close threw: {}", e.getMessage());
            }
        }
        consumers.clear();
        try { producer.close(Duration.ofSeconds(2)); } catch (RuntimeException e) {
            log.warn("Producer close threw: {}", e.getMessage());
        }
    }

    /* ===== prop builders ================================================== */

    private static Properties consumerProps(KafkaBridgeConfig cfg, String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.cluster().bootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, cfg.cluster().enableAutoCommit());
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, cfg.cluster().maxPollRecords());
        p.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,
                (int) cfg.cluster().maxPollInterval().toMillis());
        applySecurity(p, cfg.security());
        return p;
    }

    private static Properties producerProps(KafkaBridgeConfig cfg) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.cluster().bootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        applySecurity(p, cfg.security());
        return p;
    }

    private static void applySecurity(Properties p, KafkaBridgeConfig.SecurityConfig sec) {
        if (sec == null) return;
        if (sec.protocol() != null) {
            p.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, sec.protocol());
        }
        if (sec.saslMechanism() != null) {
            p.put(SaslConfigs.SASL_MECHANISM, sec.saslMechanism());
        }
        if (sec.truststore() != null) {
            p.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sec.truststore());
        }
        // OAuth, client-id/secret resolution: deployments wire jaas.config via
        // env. We deliberately do not read env vars here — the operator's
        // bootstrap script populates the JAAS string before launching the
        // worker, so secrets never live in this process's heap longer than the
        // login module needs them.
    }
}
