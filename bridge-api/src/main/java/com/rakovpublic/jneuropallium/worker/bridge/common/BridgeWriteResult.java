/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

/**
 * Result of a protocol-level write issued by an
 * {@link AbstractBridgeOutputAggregator} subclass.
 *
 * @param success {@code true} iff the protocol layer accepted the write
 *                with a good status.
 * @param detail  free-form protocol detail (e.g. an OPC UA {@code StatusCode}
 *                string, an MQTT reason code, an HTTP body fragment) used
 *                in the audit record.
 */
public record BridgeWriteResult(boolean success, String detail) {

    public static BridgeWriteResult ok() { return new BridgeWriteResult(true, null); }

    public static BridgeWriteResult ok(String detail) { return new BridgeWriteResult(true, detail); }

    public static BridgeWriteResult failed(String detail) { return new BridgeWriteResult(false, detail); }
}
