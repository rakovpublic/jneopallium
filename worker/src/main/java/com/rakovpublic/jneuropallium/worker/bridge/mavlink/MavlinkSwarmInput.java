/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} for swarm-side reads (12-MAVLINK.md §5): drains the
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PeerObservationSignal}s
 * produced by per-peer {@code GLOBAL_POSITION_INT} bindings whose
 * {@code targetSignal} is {@code PEER_OBSERVATION}.
 *
 * <p>The HEARTBEAT-driven peer table (online/offline) lives in
 * {@link MavlinkClientService} and surfaces through the advisory event
 * channel ({@code drainEvents}); {@link MavlinkEventInput} reads that
 * channel and combines it with {@code STATUSTEXT}-derived alarms.
 */
public final class MavlinkSwarmInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(2L, 1);

    private final String name;
    private final MavlinkClientService svc;
    private final List<String> bindingIds;

    public MavlinkSwarmInput(String name, MavlinkClientService svc, List<String> bindingIds) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindingIds = List.copyOf(Objects.requireNonNull(bindingIds, "bindingIds"));
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>();
        for (String b : bindingIds) out.addAll(svc.drain(b));
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
