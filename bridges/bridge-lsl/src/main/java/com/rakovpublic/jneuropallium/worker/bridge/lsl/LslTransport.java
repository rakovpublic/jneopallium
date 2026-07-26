/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import java.util.List;

/**
 * Test seam between {@link LslClientService} and {@code liblsl-Java}
 * (05-LSL.md §2 — {@code edu.ucsd.sccn:liblsl-Java}).
 *
 * <p>The bridge does not link against the native {@code liblsl} jar in the
 * worker module so that:
 *
 * <ul>
 *   <li>The compile / unit-test classpath is independent of the platform
 *       binary that {@code liblsl} ships (linux64 / osx-arm64 / win64).
 *       (05-LSL.md §10 R1.)</li>
 *   <li>Acceptance scenarios S7..S11 in §9 can be exercised against an
 *       in-memory transport without launching a real LSL multicast host.</li>
 * </ul>
 *
 * <p>Production wiring constructs an {@code LiblslLslTransport} (out of
 * scope here) that delegates {@link Inlet} / {@link Outlet} calls to
 * {@code edu.ucsd.sccn.LSL.StreamInlet} / {@code StreamOutlet}; the rest of
 * the bridge is unaware.
 */
public interface LslTransport extends AutoCloseable {

    /** A handle to one resolved LSL inlet. */
    interface Inlet extends AutoCloseable {
        /** Stream name (LSL "name" metadata field). */
        String name();
        /** Stream type (LSL "type" metadata field — {@code EEG}, {@code HRV}, …). */
        String type();
        /** Channel labels in stream order. */
        List<String> channelLabels();
        /** Channel count. */
        int channelCount();
        /** Nominal sample rate (Hz). 0 = irregular. */
        double nominalSrate();
        /**
         * Drain at most {@code max} samples from the inlet's internal buffer
         * (LSL's own thread reads from the wire). Returns the samples in
         * arrival order; an empty list means no new data is buffered.
         */
        List<Sample> pull(int max);
        /** Time correction returned by {@code stream_inlet.time_correction()}. */
        double timeCorrectionSeconds();
        /** {@code true} if the inlet still has a live publisher. */
        boolean isAlive();
        @Override void close();
    }

    /** A handle to one outlet the bridge owns and can publish onto. */
    interface Outlet extends AutoCloseable {
        String name();
        LslBridgeConfig.OutletKind kind();
        /** Push one numeric sample. {@code timestamp} is seconds since LSL epoch. */
        void pushNumeric(double[] values, double timestamp);
        /** Push one marker (irregular string sample). */
        void pushMarker(String marker, double timestamp);
        @Override void close();
    }

    /** One LSL sample = (channel-major values, source timestamp in seconds). */
    record Sample(double[] values, String marker, double timestamp) {
        public Sample(double[] values, double timestamp) {
            this(values == null ? new double[0] : values.clone(), null, timestamp);
        }
        public Sample(String marker, double timestamp) {
            this(new double[0], marker, timestamp);
        }
    }

    /**
     * Resolve a stream by name + type within {@code timeoutMs}. Returns
     * {@code null} if no publisher answered.
     */
    Inlet resolveInlet(String name, String type, long timeoutMs);

    /** Open an outlet of the given name / kind / nominal rate. */
    Outlet openOutlet(String name, LslBridgeConfig.OutletKind kind,
                      double nominalSrate, List<String> channelLabels);

    /**
     * The LSL local clock in seconds — the canonical timestamp source on
     * the host (05-LSL.md §4 architecture diagram).
     */
    double localClockSeconds();

    /** Released on bridge shutdown. */
    @Override void close();

    /** Wraps any liblsl error so the bridge can audit it uniformly. */
    final class LslTransportException extends RuntimeException {
        public LslTransportException(String message) { super(message); }
        public LslTransportException(String message, Throwable cause) { super(message, cause); }
    }
}
