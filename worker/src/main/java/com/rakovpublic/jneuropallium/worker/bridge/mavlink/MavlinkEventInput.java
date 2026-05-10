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
 * {@link IInitInput} for the bridge-level event/alarm channel
 * (12-MAVLINK.md §5): drains the {@code AlarmSignal}s produced by
 * {@code STATUSTEXT}, {@code SYS_STATUS}, {@code RADIO_STATUS} bindings as
 * well as the {@code BRIDGE_RECONNECTED} and {@code PEER_OFFLINE} advisory
 * alarms emitted by {@link MavlinkClientService} itself.
 */
public final class MavlinkEventInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final MavlinkClientService svc;
    private final List<String> bindingIds;

    public MavlinkEventInput(String name, MavlinkClientService svc, List<String> bindingIds) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindingIds = bindingIds == null ? List.of() : List.copyOf(bindingIds);
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>(svc.drainEvents());
        for (String b : bindingIds) out.addAll(svc.drain(b));
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
