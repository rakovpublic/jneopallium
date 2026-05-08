/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import java.time.Duration;

/**
 * Stateful exponential-backoff reconnect policy (00-FRAMEWORK §2.3).
 *
 * <p>Doubles the previous delay on every consecutive failure, capped at
 * {@code maxDelay} (default 30 s). {@link #reset()} on a successful
 * connect.
 *
 * <p>Not thread-safe — caller is the bridge's reconnect coroutine, which
 * is single-threaded by construction.
 */
public final class BridgeReconnectPolicy {

    /** Default initial delay (1 s). */
    public static final Duration DEFAULT_INITIAL = Duration.ofSeconds(1);

    /** Default cap (30 s, per §2.3). */
    public static final Duration DEFAULT_MAX = Duration.ofSeconds(30);

    private final Duration initial;
    private final Duration max;
    private int attempt;

    public BridgeReconnectPolicy() { this(DEFAULT_INITIAL, DEFAULT_MAX); }

    public BridgeReconnectPolicy(Duration initial, Duration max) {
        if (initial == null || initial.isNegative() || initial.isZero())
            throw new IllegalArgumentException("initial must be > 0");
        if (max == null || max.compareTo(initial) < 0)
            throw new IllegalArgumentException("max must be >= initial");
        this.initial = initial;
        this.max = max;
        this.attempt = 0;
    }

    /** Number of consecutive failed reconnects since the last reset. */
    public int attempt() { return attempt; }

    /** Returns the next delay and increments the attempt counter. */
    public Duration nextDelay() {
        long initMs = initial.toMillis();
        long capMs = max.toMillis();
        // shift safely; saturate at cap
        long ms = (attempt >= 31) ? capMs
                : Math.min(capMs, initMs << attempt);
        attempt++;
        return Duration.ofMillis(ms);
    }

    /** Reset the backoff state after a successful connect. */
    public void reset() { attempt = 0; }
}
