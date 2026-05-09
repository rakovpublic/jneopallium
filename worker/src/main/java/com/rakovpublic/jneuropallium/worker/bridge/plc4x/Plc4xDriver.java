/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import java.util.Map;

/**
 * Abstraction over the Apache PLC4X client surface used by the bridge
 * (01-PLC4X.md §3). Implementations:
 *
 * <ul>
 *   <li>Production: a thin adapter over
 *       {@code org.apache.plc4x.java.api.PlcDriverManager} +
 *       {@code PlcConnection} +
 *       {@code PlcReadRequest}/{@code PlcWriteRequest}. Lives outside this
 *       module so the core does not depend on the PLC4X jar.</li>
 *   <li>Tests: {@code StubPlc4xDriver} — a pure-Java in-memory PLC simulator
 *       that supports both {@code s7://} and {@code modbus-tcp://} schemes.</li>
 * </ul>
 *
 * <h2>Threading</h2>
 * Implementations must be thread-safe. {@link #read} and {@link #write} are
 * called from the per-connection polling thread and the aggregator's tick
 * thread respectively.
 *
 * <h2>Lifecycle</h2>
 * {@link #open(String, String)} is called once per connection at bridge
 * startup. {@link #close(String)} is called per connection at shutdown.
 * {@link #closeAll()} releases everything still open.
 */
public interface Plc4xDriver extends AutoCloseable {

    /**
     * Open a connection.
     *
     * @param connectionId stable identifier from the YAML config
     * @param connectionString PLC4X connection string
     *                         ({@code s7://10.10.0.1?remote-rack=0&remote-slot=1},
     *                         {@code modbus-tcp://10.10.0.2:502?unit-identifier=1}, …)
     * @throws Plc4xException if the scheme has no registered driver (S10) or
     *                        the controller refuses the connection
     */
    void open(String connectionId, String connectionString);

    /**
     * Validate a field expression against an open connection by issuing a
     * one-shot read (01-PLC4X.md §5 last paragraph: "fail fast at config
     * load if any address is rejected"). Returns the response code; the
     * caller treats anything other than {@link Plc4xResponseCode#OK} as a
     * fatal startup error (S9).
     */
    Plc4xResponseCode validate(String connectionId, String fieldAddress);

    /**
     * Read a single field. The returned response always has a code; the
     * value may be {@code null} if the code is non-OK.
     */
    ReadResponse read(String connectionId, String fieldAddress);

    /**
     * Write a single field.
     */
    Plc4xResponseCode write(String connectionId, String fieldAddress, Object value);

    /** Close one connection. Safe to call on an unknown id (no-op). */
    void close(String connectionId);

    /** Close all open connections. */
    void closeAll();

    @Override
    default void close() { closeAll(); }

    /**
     * Result of a single-field read.
     *
     * <p>{@code value} is one of: {@link Boolean}, {@link Number},
     * {@link String}, or {@link Map} (for bit-decoded WORDs). May be
     * {@code null} when {@code code != OK}.
     *
     * @param code  protocol-level response code
     * @param value protocol-native value (or {@code null} on error)
     */
    record ReadResponse(Plc4xResponseCode code, Object value) {
        public static ReadResponse ok(Object value) {
            return new ReadResponse(Plc4xResponseCode.OK, value);
        }
        public static ReadResponse failure(Plc4xResponseCode code) {
            return new ReadResponse(code, null);
        }
    }
}
