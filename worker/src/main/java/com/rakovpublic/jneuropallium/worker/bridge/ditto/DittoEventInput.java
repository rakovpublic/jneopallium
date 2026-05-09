/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains {@link AlarmSignal}s emitted by
 * {@link DittoClientService} from {@code THING_DELETED} or
 * {@code BRIDGE_RECONNECTED} conditions (10-DITTO.md §4).
 */
public final class DittoEventInput implements IInitInput {

    private final String name;
    private final DittoClientService svc;

    public DittoEventInput(String name, DittoClientService svc) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
    }

    @Override public List<IInputSignal> readSignals() { return svc.drainEvents(); }
    @Override public String getName() { return name; }
    @Override public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }
    @Override public ProcessingFrequency getDefaultProcessingFrequency() {
        return AlarmSignal.PROCESSING_FREQUENCY;
    }
}
