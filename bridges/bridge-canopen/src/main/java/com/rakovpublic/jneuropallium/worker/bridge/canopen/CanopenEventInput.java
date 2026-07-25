/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains advisory event signals ({@code AlarmSignal}
 * for EMCY, drive faults, heartbeat-loss, and {@code BRIDGE_RECONNECTED})
 * from {@link CanopenClientService} (13-CANOPEN.md §5).
 *
 * <p>This input drains the global event channel rather than per-binding
 * queues, so a single instance covers the whole bridge.
 */
public final class CanopenEventInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final CanopenClientService svc;

    public CanopenEventInput(String name, CanopenClientService svc) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
    }

    @Override
    public List<IInputSignal> readSignals() {
        return new ArrayList<>(svc.drainEvents());
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
