/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains IEC 61850 read bindings
 * (11-IEC61850.md §7 — {@code Iec61850MeasurementInput}).
 *
 * <p>The {@link Iec61850ClientService} polls the configured Data Attributes
 * once per tick and decodes them via {@link Iec61850SignalMapper}; this
 * adapter only walks the configured binding-id list and snapshots their
 * queues. {@code getDesiredResults()} is empty — the bridge is read-only
 * and has no aggregator (§7).
 */
public final class Iec61850MeasurementInput implements IInitInput {

    /** Aligned with {@link MeasurementSignal#PROCESSING_FREQUENCY} so the
     *  bridge pace matches the consumer neuron's cadence. */
    public static final ProcessingFrequency PROCESSING_FREQUENCY = MeasurementSignal.PROCESSING_FREQUENCY;

    private final String name;
    private final Iec61850ClientService svc;
    private final List<String> bindingIds;

    public Iec61850MeasurementInput(String name, Iec61850ClientService svc, List<String> bindingIds) {
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
