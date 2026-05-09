/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

/**
 * Thrown when the FMI layer returns a non-recoverable status (ERROR, FATAL)
 * or when FMU lifecycle methods fail (03-FMI-FMU.md §9 scenario S11).
 */
public final class FmuException extends RuntimeException {

    private final FmuDriver.FmiStatus status;

    public FmuException(String message, FmuDriver.FmiStatus status) {
        super(message);
        this.status = status;
    }

    public FmuException(String message) {
        this(message, FmuDriver.FmiStatus.ERROR);
    }

    public FmuDriver.FmiStatus status() { return status; }
}
