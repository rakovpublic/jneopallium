/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory {@link LslTransport} used by 05-LSL.md §9 S7..S11. Mimics
 * resolution, channel layout, sample buffering, time correction and the
 * stream-lost transition without launching mDNS multicast or linking
 * against {@code liblsl-Java}.
 */
final class InMemoryLslTransport implements LslTransport {

    private final Map<String, Stream> streams = new LinkedHashMap<>();
    private final Map<String, Outlet> outletsByName = new LinkedHashMap<>();
    private double clockSeconds = 0.0;
    private boolean closed;

    /** Pre-register a publishable LSL stream (the S7 publisher fixture). */
    void publishStream(String name, String type, List<String> channels, double nominalSrate) {
        streams.put(name, new Stream(name, type, channels, nominalSrate));
    }

    /** Push a numeric sample onto a previously-published stream's inbox. */
    void publishSample(String streamName, double[] values, double timestamp) {
        Stream s = streams.get(streamName);
        if (s == null) throw new IllegalArgumentException("stream not published: " + streamName);
        s.samples.add(new Sample(values, timestamp));
    }

    /** Push a marker onto a previously-published stream's inbox. */
    void publishMarker(String streamName, String marker, double timestamp) {
        Stream s = streams.get(streamName);
        if (s == null) throw new IllegalArgumentException("stream not published: " + streamName);
        s.samples.add(new Sample(marker, timestamp));
    }

    /** S9 — kill the publisher; subsequent {@code isAlive()} calls return false. */
    void killPublisher(String streamName) {
        Stream s = streams.get(streamName);
        if (s != null) s.alive = false;
    }

    /** Move the local clock forward (seconds). */
    void tickClock(double deltaSeconds) { this.clockSeconds += deltaSeconds; }

    void setTimeCorrection(String streamName, double tc) {
        Stream s = streams.get(streamName);
        if (s != null) s.timeCorrection = tc;
    }

    /** Snapshot of every marker pushed onto a particular outlet. */
    List<String> markersOn(String outletName) {
        Outlet o = outletsByName.get(outletName);
        return o == null ? List.of() : List.copyOf(o.markers);
    }

    /** Snapshot of every numeric sample pushed onto a particular outlet. */
    List<double[]> numericOn(String outletName) {
        Outlet o = outletsByName.get(outletName);
        return o == null ? List.of() : List.copyOf(o.numericSamples);
    }

    @Override
    public Inlet resolveInlet(String name, String type, long timeoutMs) {
        Stream s = streams.get(name);
        if (s == null) return null;
        if (type != null && s.type != null && !type.equals(s.type)) return null;
        return new InletImpl(s);
    }

    @Override
    public Outlet openOutlet(String name, LslBridgeConfig.OutletKind kind,
                             double nominalSrate, List<String> channelLabels) {
        Outlet o = new Outlet(name, kind, nominalSrate);
        outletsByName.put(name, o);
        return o;
    }

    @Override
    public double localClockSeconds() { return clockSeconds; }

    @Override
    public void close() { this.closed = true; }

    boolean isClosed() { return closed; }

    /* ===== inner types ==================================================== */

    private static final class Stream {
        final String name, type;
        final List<String> channels;
        final double nominalSrate;
        boolean alive = true;
        double timeCorrection = 0.0;
        final List<Sample> samples = new ArrayList<>();

        Stream(String name, String type, List<String> channels, double nominalSrate) {
            this.name = name;
            this.type = type;
            this.channels = channels == null ? List.of() : List.copyOf(channels);
            this.nominalSrate = nominalSrate;
        }
    }

    private static final class InletImpl implements Inlet {
        private final Stream stream;
        private boolean closed;
        InletImpl(Stream s) { this.stream = s; }
        @Override public String name() { return stream.name; }
        @Override public String type() { return stream.type; }
        @Override public List<String> channelLabels() { return stream.channels; }
        @Override public int channelCount() { return stream.channels.size(); }
        @Override public double nominalSrate() { return stream.nominalSrate; }
        @Override public List<Sample> pull(int max) {
            if (closed) return List.of();
            int n = Math.min(max, stream.samples.size());
            if (n <= 0) return List.of();
            List<Sample> snap = new ArrayList<>(n);
            for (int i = 0; i < n; i++) snap.add(stream.samples.remove(0));
            return snap;
        }
        @Override public double timeCorrectionSeconds() { return stream.timeCorrection; }
        @Override public boolean isAlive() { return stream.alive && !closed; }
        @Override public void close() { closed = true; }
    }

    static final class Outlet implements LslTransport.Outlet {
        final String name;
        final LslBridgeConfig.OutletKind kind;
        final double nominalSrate;
        final List<String> markers = new ArrayList<>();
        final List<double[]> numericSamples = new ArrayList<>();

        Outlet(String name, LslBridgeConfig.OutletKind kind, double nominalSrate) {
            this.name = name;
            this.kind = kind;
            this.nominalSrate = nominalSrate;
        }

        @Override public String name() { return name; }
        @Override public LslBridgeConfig.OutletKind kind() { return kind; }
        @Override public void pushNumeric(double[] values, double timestamp) {
            numericSamples.add(values == null ? new double[0] : values.clone());
        }
        @Override public void pushMarker(String marker, double timestamp) { markers.add(marker); }
        @Override public void close() { /* no-op */ }
    }
}
