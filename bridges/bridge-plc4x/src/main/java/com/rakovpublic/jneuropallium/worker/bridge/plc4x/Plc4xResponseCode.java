/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

/**
 * Mirrors the subset of {@code org.apache.plc4x.java.api.types.PlcResponseCode}
 * used by the bridge. Decoupled from the real PLC4X jar so the bridge core
 * (and its tests) compile without that dependency on the classpath.
 *
 * <p>A production driver adapter ({@link Plc4xDriver}) maps the real
 * {@code PlcResponseCode} onto these values; the in-memory test stub uses
 * them directly.
 */
public enum Plc4xResponseCode {
    /** Read or write completed successfully. */
    OK,
    /** Address not found in the controller's address map. */
    NOT_FOUND,
    /** Authenticated but not authorised to read/write the address. */
    ACCESS_DENIED,
    /** Address is valid but the value is currently invalid (e.g. uninitialised DB). */
    INVALID_ADDRESS,
    /** Value present but its data type does not match the requested type. */
    INVALID_DATATYPE,
    /** Value out of range for the requested type. */
    INVALID_DATA,
    /** Internal driver error. */
    INTERNAL_ERROR,
    /** Connection lost / IO error during the operation. */
    REMOTE_ERROR,
    /** Operation timed out. */
    RESPONSE_PENDING
}
