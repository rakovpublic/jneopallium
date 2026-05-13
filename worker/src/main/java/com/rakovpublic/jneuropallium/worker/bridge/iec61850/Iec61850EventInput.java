/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains IEC 61850 event bindings — Report Control
 * Block reports (and, later, GOOSE multicast) translated to
 * {@link AlarmSignal}s (11-IEC61850.md §7 — {@code Iec61850EventInput}).
 *
 * <p>11-IEC61850.md §8 phase 3 deliberately defers GOOSE listener wiring
 * (separate thread, raw socket / VLAN config); the bridge phase plan
 * keeps event ingestion to Report Control Blocks for v1. The
 * {@link Iec61850ClientService} routes both into the same per-binding
 * queues — adding a GOOSE listener does not change this adapter.
 */
public final class Iec61850EventInput implements IInitInput {

    /** Aligned with {@link AlarmSignal#PROCESSING_FREQUENCY}. */
    public static final ProcessingFrequency PROCESSING_FREQUENCY = AlarmSignal.PROCESSING_FREQUENCY;

    private final String name;
    private final Iec61850ClientService svc;
    private final List<String> bindingIds;

    public Iec61850EventInput(String name, Iec61850ClientService svc, List<String> bindingIds) {
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
