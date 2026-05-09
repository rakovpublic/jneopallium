/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pure-Java in-memory PLC simulator used by the bridge integration tests.
 *
 * <h2>What it models</h2>
 * <ul>
 *   <li>Multiple "connections", each opened with a connection-string scheme
 *       (e.g. {@code s7://}, {@code modbus-tcp://}). Schemes outside
 *       {@link #ALLOWED_SCHEMES} fail to open with a helpful message,
 *       exercising S10 ("driver missing").</li>
 *   <li>A per-(connection,address) value map writable via
 *       {@link #setValue(String, String, Object)} for test setup.</li>
 *   <li>An optional rejected-address set per connection
 *       ({@link #rejectAddress(String, String)}) for S9
 *       ("address rejected at startup").</li>
 *   <li>Per-binding read counters for cadence asserts.</li>
 *   <li>A "drop connection" hook that flips reads/writes to REMOTE_ERROR
 *       until the connection is restored.</li>
 * </ul>
 */
public final class StubPlc4xDriver implements Plc4xDriver {

    /** Schemes for which a driver is "registered". S10 simulates a missing driver. */
    public static final java.util.Set<String> ALLOWED_SCHEMES =
            java.util.Set.of("s7", "modbus-tcp", "ethernet-ip", "ads", "stub");

    /** open connections: id → connection string. */
    private final Map<String, String> open = new ConcurrentHashMap<>();
    /** disconnected connections: id → true. Reads/writes fail with REMOTE_ERROR. */
    private final Map<String, Boolean> disconnected = new ConcurrentHashMap<>();
    /** value cache: id → (address → value). */
    private final Map<String, Map<String, Object>> values = new ConcurrentHashMap<>();
    /** addresses to reject at validate(): id → set of addresses. */
    private final Map<String, java.util.Set<String>> rejected = new ConcurrentHashMap<>();
    /** per-binding read counters. */
    private final Map<String, AtomicLong> readCounts = new ConcurrentHashMap<>();
    /** per-binding write log (for assertions). */
    private final Map<String, Object> lastWrites = new ConcurrentHashMap<>();

    @Override
    public void open(String connectionId, String connectionString) {
        Objects.requireNonNull(connectionId);
        Objects.requireNonNull(connectionString);
        String scheme = scheme(connectionString);
        if (!ALLOWED_SCHEMES.contains(scheme)) {
            throw new Plc4xException("no driver registered for scheme '" + scheme + "'");
        }
        open.put(connectionId, connectionString);
        values.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>());
        disconnected.remove(connectionId);
    }

    @Override
    public Plc4xResponseCode validate(String connectionId, String fieldAddress) {
        if (!open.containsKey(connectionId)) return Plc4xResponseCode.REMOTE_ERROR;
        java.util.Set<String> rs = rejected.get(connectionId);
        if (rs != null && rs.contains(fieldAddress)) return Plc4xResponseCode.INVALID_ADDRESS;
        return Plc4xResponseCode.OK;
    }

    @Override
    public ReadResponse read(String connectionId, String fieldAddress) {
        if (!open.containsKey(connectionId)) {
            return ReadResponse.failure(Plc4xResponseCode.REMOTE_ERROR);
        }
        if (Boolean.TRUE.equals(disconnected.get(connectionId))) {
            return ReadResponse.failure(Plc4xResponseCode.REMOTE_ERROR);
        }
        readCounts.computeIfAbsent(fieldAddress, k -> new AtomicLong()).incrementAndGet();
        Object v = values.getOrDefault(connectionId, Map.of()).get(fieldAddress);
        if (v == null) return ReadResponse.failure(Plc4xResponseCode.NOT_FOUND);
        return ReadResponse.ok(v);
    }

    @Override
    public Plc4xResponseCode write(String connectionId, String fieldAddress, Object value) {
        if (!open.containsKey(connectionId)) return Plc4xResponseCode.REMOTE_ERROR;
        if (Boolean.TRUE.equals(disconnected.get(connectionId))) return Plc4xResponseCode.REMOTE_ERROR;
        values.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>())
                .put(fieldAddress, value);
        lastWrites.put(connectionId + "|" + fieldAddress, value);
        return Plc4xResponseCode.OK;
    }

    @Override
    public void close(String connectionId) {
        open.remove(connectionId);
        disconnected.remove(connectionId);
    }

    @Override
    public void closeAll() {
        open.clear();
        disconnected.clear();
    }

    /* ===== test helpers ===================================================== */

    /** Set a tag value on a (connection, address). */
    public void setValue(String connectionId, String fieldAddress, Object value) {
        values.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>())
                .put(fieldAddress, value);
    }

    /** Mark an address as rejected at validate() — simulates a typo'd address. */
    public void rejectAddress(String connectionId, String fieldAddress) {
        rejected.computeIfAbsent(connectionId, k -> ConcurrentHashMap.newKeySet())
                .add(fieldAddress);
    }

    /** Read count for a given address (sum across all connections). */
    public long readCount(String fieldAddress) {
        AtomicLong n = readCounts.get(fieldAddress);
        return n == null ? 0L : n.get();
    }

    /** Last value written by the bridge to (connection, address), or {@code null}. */
    public Object lastWrite(String connectionId, String fieldAddress) {
        return lastWrites.get(connectionId + "|" + fieldAddress);
    }

    /** Simulate a network drop. {@link #restoreConnection(String)} undoes it. */
    public void dropConnection(String connectionId) {
        disconnected.put(connectionId, Boolean.TRUE);
    }

    public void restoreConnection(String connectionId) {
        disconnected.remove(connectionId);
    }

    public boolean isOpen(String connectionId) {
        return open.containsKey(connectionId);
    }

    private static String scheme(String connectionString) {
        int i = connectionString.indexOf("://");
        return i < 0 ? connectionString : connectionString.substring(0, i);
    }
}
