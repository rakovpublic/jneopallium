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
 * {@link IInitInput} that drains decoded telemetry signals
 * ({@link com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal}
 * and
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal})
 * from {@link MavlinkClientService} (12-MAVLINK.md §5).
 *
 * <p>One instance binds to one or more PROPRIOCEPTIVE/EFFICIENCY-class read
 * bindings. {@code readSignals()} returns a snapshot — the connection
 * service maintains the cache asynchronously; this method does not block
 * on the network.
 */
public final class MavlinkTelemetryInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final MavlinkClientService svc;
    private final List<String> bindingIds;

    public MavlinkTelemetryInput(String name, MavlinkClientService svc, List<String> bindingIds) {
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
