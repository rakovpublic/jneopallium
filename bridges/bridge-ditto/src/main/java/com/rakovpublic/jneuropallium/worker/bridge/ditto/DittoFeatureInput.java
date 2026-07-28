/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

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
 * {@link IInitInput} that drains decoded {@link MeasurementSignal}s from
 * {@link DittoClientService} (10-DITTO.md §4 ingress mapping).
 *
 * <p>One instance binds to one or more read bindings whose
 * {@link DittoBridgeConfig.ReadSignalKind} is {@code MEASUREMENT}.
 * {@code readSignals()} returns a snapshot — the connection service
 * maintains the cache asynchronously; this method does not block on the
 * network (00-FRAMEWORK §2.1).
 */
public final class DittoFeatureInput implements IInitInput {

    private final String name;
    private final DittoClientService svc;
    private final List<String> bindingIds;

    public DittoFeatureInput(String name, DittoClientService svc, List<String> bindingIds) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindingIds = List.copyOf(Objects.requireNonNull(bindingIds, "bindingIds"));
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>();
        for (String b : bindingIds) out.addAll(svc.drainMeasurements(b));
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return MeasurementSignal.PROCESSING_FREQUENCY;
    }
}
