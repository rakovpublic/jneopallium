/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pure-Java in-memory {@link KafkaTransport} for the bridge integration tests.
 *
 * <p>Runs without a broker, JMX, JNDI, or docker. Mirrors enough of Kafka's
 * semantics to validate the scenarios in 08-KAFKA.md §8:
 * <ul>
 *   <li>Per-topic offset/partition log appended via {@link #produceTo}.</li>
 *   <li>Per-binding consumer position; {@link #poll} returns up to
 *       {@code maxRecords} from {@code position} forward.</li>
 *   <li>{@link #commitSync} advances the persistent committed offset; a
 *       {@link #simulateCrash(String)} drops the in-memory position so the
 *       next subscriber re-receives uncommitted records (S9).</li>
 *   <li>{@link #addInstance(String)} adds a second member to a consumer
 *       group; partitions are reassigned across members so two bridges
 *       cooperatively process one topic without duplicating signals (S12).</li>
 *   <li>{@link #failNextSend(String, int)} flips the producer to throw
 *       a configurable number of times to exercise audit FAILED records.</li>
 * </ul>
 *
 * <p>Kept inside the test tree intentionally — production code uses
 * {@link DefaultKafkaTransport}.
 */
public final class InMemoryKafkaTransport implements KafkaTransport {

    /** Topic → partition log. Single-partition topics: partition is always 0. */
    private final Map<String, List<InboundRecord>> log = new ConcurrentHashMap<>();

    /** Per-binding read position (next offset to deliver). */
    private final Map<String, AtomicLong> position = new ConcurrentHashMap<>();

    /** Per-binding committed offset (last commitSync()). */
    private final Map<String, AtomicLong> committed = new ConcurrentHashMap<>();

    /** Per-binding subscribed topic. */
    private final Map<String, String> subscriptions = new LinkedHashMap<>();

    /** Per-binding consumer group id. */
    private final Map<String, String> groups = new LinkedHashMap<>();

    /** All produced records by topic, in send order. Tests assert on this list. */
    private final Map<String, List<InboundRecord>> produced = new ConcurrentHashMap<>();

    /** Counter used to fail send() N times (S11 + producer-failure audit path). */
    private final Map<String, AtomicLong> failsRemaining = new ConcurrentHashMap<>();

    /** Group instances registered for partition-redistribution sim (S12). */
    private final Map<String, List<String>> groupMembers = new ConcurrentHashMap<>();

    /** Per-binding stripe to support S12 partition redistribution among group members. */
    private final Map<String, Integer> stripeOfBinding = new ConcurrentHashMap<>();

    @Override
    public synchronized void subscribe(String bindingId, String topic, String groupId) {
        subscriptions.put(bindingId, topic);
        groups.put(bindingId, groupId);
        position.computeIfAbsent(bindingId, k -> new AtomicLong(0));
        committed.computeIfAbsent(bindingId, k -> new AtomicLong(0));
        log.computeIfAbsent(topic, k -> new ArrayList<>());
        groupMembers.computeIfAbsent(groupId, k -> new ArrayList<>()).add(bindingId);
        rebalanceStripe(groupId);
    }

    @Override
    public synchronized List<InboundRecord> poll(String bindingId, Duration timeout, int maxRecords) {
        String topic = subscriptions.get(bindingId);
        if (topic == null) throw new KafkaTransportException("unknown binding " + bindingId);
        List<InboundRecord> partitionLog = log.get(topic);
        if (partitionLog == null || partitionLog.isEmpty()) return List.of();

        AtomicLong pos = position.get(bindingId);
        long start = pos.get();
        Integer stripe = stripeOfBinding.get(bindingId);
        Integer mod = groupMembers.getOrDefault(groups.get(bindingId), List.of(bindingId)).size();

        List<InboundRecord> out = new ArrayList<>();
        long i = start;
        while (i < partitionLog.size() && out.size() < maxRecords) {
            // S12: round-robin across group members.
            if (mod > 1 && stripe != null && (i % mod) != stripe) {
                i++;
                continue;
            }
            out.add(partitionLog.get((int) i));
            i++;
        }
        // The "position" tracks how far this binding has *delivered* — committed
        // tracks how far it's been *acknowledged*. simulateCrash() rolls position
        // back to committed so the next poll re-delivers uncommitted records.
        pos.set(i);
        return out;
    }

    @Override
    public synchronized void commitSync(String bindingId, Map<Integer, Long> partitionToOffset) {
        Long o = partitionToOffset.get(0);
        if (o != null) committed.get(bindingId).set(o);
    }

    @Override
    public synchronized void send(String topic, String key, byte[] value) {
        AtomicLong fails = failsRemaining.get(topic);
        if (fails != null && fails.get() > 0) {
            fails.decrementAndGet();
            throw new KafkaTransportException("simulated producer failure on " + topic);
        }
        InboundRecord rec = new InboundRecord(
                topic, 0,
                log.computeIfAbsent(topic, k -> new ArrayList<>()).size(),
                key, value);
        log.get(topic).add(rec);
        produced.computeIfAbsent(topic, k -> new ArrayList<>()).add(rec);
    }

    @Override
    public synchronized void close() { /* nothing to release */ }

    /* ===== test helpers ===================================================== */

    /** Append a record to a topic outside the producer path (simulates an upstream broker). */
    public synchronized void produceTo(String topic, String key, byte[] value) {
        InboundRecord rec = new InboundRecord(
                topic, 0,
                log.computeIfAbsent(topic, k -> new ArrayList<>()).size(),
                key, value);
        log.get(topic).add(rec);
    }

    /** Return everything ever sent via {@link #send} (in send order). */
    public synchronized List<InboundRecord> produced(String topic) {
        return new ArrayList<>(produced.getOrDefault(topic, List.of()));
    }

    /** Make the next {@code n} {@link #send}s throw — used to test PRODUCER_ERROR audits. */
    public void failNextSend(String topic, int n) {
        failsRemaining.computeIfAbsent(topic, k -> new AtomicLong()).set(n);
    }

    /**
     * Drop the binding's in-flight position back to the last committed offset —
     * simulating a process crash mid-batch (S9: at-least-once).
     */
    public synchronized void simulateCrash(String bindingId) {
        AtomicLong pos = position.get(bindingId);
        AtomicLong com = committed.get(bindingId);
        if (pos != null && com != null) pos.set(com.get());
    }

    /** Number of records currently committed for a binding. */
    public long committedOffset(String bindingId) {
        AtomicLong a = committed.get(bindingId);
        return a == null ? 0 : a.get();
    }

    /** Add a second binding to the same group as {@code existing}, simulating a new bridge instance. */
    public synchronized void addInstance(String existingBinding, String newBinding, String topic) {
        String group = groups.get(existingBinding);
        Objects.requireNonNull(group, "existing binding has no group");
        subscribe(newBinding, topic, group);
    }

    /** Recompute the stripe-id of every binding in this group so the next poll honours new membership. */
    private synchronized void rebalanceStripe(String groupId) {
        List<String> members = groupMembers.get(groupId);
        Map<String, Integer> stripes = new HashMap<>();
        for (int i = 0; i < members.size(); i++) stripes.put(members.get(i), i);
        for (Map.Entry<String, Integer> e : stripes.entrySet()) {
            stripeOfBinding.put(e.getKey(), e.getValue());
        }
    }
}
