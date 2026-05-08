/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe TTL-keyed registry of operator overrides (00-FRAMEWORK §2.2,
 * §6). Per ground rule §0.3 an entry blocks neuron-derived writes to the
 * tag for the duration of the override; expiry releases the tag back to the
 * aggregator on the next tick.
 */
public final class OverrideRegistry {

    /** A single override record kept in the registry. */
    public record Entry(
            String tag,
            String operatorId,
            String reason,
            double manualValue,
            long expiresAtEpochMs
    ) {
        public Entry {
            Objects.requireNonNull(tag, "tag");
        }

        public boolean isActiveAt(long nowEpochMs) {
            return expiresAtEpochMs > nowEpochMs;
        }
    }

    private final ConcurrentMap<String, Entry> byTag = new ConcurrentHashMap<>();

    /** Insert / refresh an override. {@code ttlMillis} {@code <= 0} clears it. */
    public void put(String tag, String operatorId, String reason,
                    double manualValue, long ttlMillis) {
        put(tag, operatorId, reason, manualValue, ttlMillis, System.currentTimeMillis());
    }

    /** Same as {@link #put(String, String, String, double, long)} with an injected clock. */
    public void put(String tag, String operatorId, String reason,
                    double manualValue, long ttlMillis, long nowEpochMs) {
        Objects.requireNonNull(tag, "tag");
        if (ttlMillis <= 0) { byTag.remove(tag); return; }
        byTag.put(tag, new Entry(tag, operatorId, reason, manualValue,
                Math.addExact(nowEpochMs, ttlMillis)));
    }

    /** Remove the override for {@code tag}, if any. */
    public void clear(String tag) { byTag.remove(tag); }

    /** Drop all entries. */
    public void clearAll() { byTag.clear(); }

    /** Active override for {@code tag} at {@code nowEpochMs}, expiring stale ones lazily. */
    public Optional<Entry> active(String tag, long nowEpochMs) {
        Entry e = byTag.get(tag);
        if (e == null) return Optional.empty();
        if (!e.isActiveAt(nowEpochMs)) {
            byTag.remove(tag, e);
            return Optional.empty();
        }
        return Optional.of(e);
    }

    /** Convenience: {@code active(tag, System.currentTimeMillis()).isPresent()}. */
    public boolean isActive(String tag) {
        return active(tag, System.currentTimeMillis()).isPresent();
    }

    /** Number of currently held entries (including any past-TTL entries that have not been swept yet). */
    public int size() { return byTag.size(); }
}
