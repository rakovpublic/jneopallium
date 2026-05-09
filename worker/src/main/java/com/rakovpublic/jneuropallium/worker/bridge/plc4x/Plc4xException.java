/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

/**
 * Thrown when a PLC4X driver call fails non-recoverably (connection refused,
 * unknown scheme, malformed field expression rejected at startup — see S9/S10
 * in 01-PLC4X.md §8).
 */
public final class Plc4xException extends RuntimeException {

    private final Plc4xResponseCode responseCode;

    public Plc4xException(String message) {
        this(message, null, null);
    }

    public Plc4xException(String message, Throwable cause) {
        this(message, null, cause);
    }

    public Plc4xException(String message, Plc4xResponseCode responseCode) {
        this(message, responseCode, null);
    }

    public Plc4xException(String message, Plc4xResponseCode responseCode, Throwable cause) {
        super(message, cause);
        this.responseCode = responseCode;
    }

    /** The protocol response code that triggered the exception, or {@code null} if unknown. */
    public Plc4xResponseCode responseCode() { return responseCode; }
}
