/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains marker-stream bindings (05-LSL.md §5,
 * §7 — {@code LslMarkerInput}).
 *
 * <p>The mapper inside {@link LslSignalMapper#toCalibration} pseudonymises
 * the marker text (per §10 R4) and emits a {@code CalibrationSignal} when
 * the configured cue regex matches; non-matching markers are dropped.
 * Bridge-level events (LSL_STREAM_LOST, BRIDGE_RECONNECTED) are drained
 * separately via {@link LslClientService#drainEvents()} and surfaced on
 * a sibling {@link com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput}.
 */
public final class LslMarkerInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 3);

    private final String name;
    private final LslClientService svc;
    private final List<String> bindingIds;
    private final boolean includeBridgeEvents;

    public LslMarkerInput(String name, LslClientService svc,
                          List<String> bindingIds, boolean includeBridgeEvents) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindingIds = List.copyOf(Objects.requireNonNull(bindingIds, "bindingIds"));
        this.includeBridgeEvents = includeBridgeEvents;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>();
        for (String b : bindingIds) out.addAll(svc.drain(b));
        if (includeBridgeEvents) out.addAll(svc.drainEvents());
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
