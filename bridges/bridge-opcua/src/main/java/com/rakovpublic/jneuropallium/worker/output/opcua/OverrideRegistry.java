/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.output.opcua;

import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.OperatorOverrideSignal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory map of active operator overrides keyed by tag. Each entry has
 * a wall-clock expiry; {@link #expireOlderThan} drops entries whose
 * deadline has passed.
 */
public final class OverrideRegistry {

    private static final long DEFAULT_DURATION_MILLIS = 5 * 60_000L;

    private final Map<String, Entry> active = new ConcurrentHashMap<>();

    public void record(OperatorOverrideSignal s, long ts) {
        if (s == null || s.getTag() == null) return;
        active.put(s.getTag(), new Entry(s, ts + DEFAULT_DURATION_MILLIS));
    }

    public boolean isActive(String tag, long now) {
        Entry e = active.get(tag);
        if (e == null) return false;
        if (e.expiresAt < now) { active.remove(tag); return false; }
        return true;
    }

    public OperatorOverrideSignal active(String tag) {
        Entry e = active.get(tag);
        return e == null ? null : e.signal;
    }

    public void expireOlderThan(long now) {
        active.values().removeIf(e -> e.expiresAt < now);
    }

    public int size() { return active.size(); }

    private record Entry(OperatorOverrideSignal signal, long expiresAt) {}
}
