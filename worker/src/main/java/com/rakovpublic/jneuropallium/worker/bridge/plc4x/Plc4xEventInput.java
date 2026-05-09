/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

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
 * Snapshot-based {@link IInitInput} that turns each polled alarm word into
 * one {@link AlarmSignal} per matching bit (01-PLC4X.md §4, §5 severity map;
 * S11 acceptance scenario).
 *
 * <p>Inactive bits (mask not set in the polled value) produce no signal.
 * Cleared alarms simply stop appearing — no separate "clear" event is
 * synthesised by the bridge.
 */
public final class Plc4xEventInput implements IInitInput {

    private final String name;
    private final Plc4xClientService svc;
    private final List<Plc4xConfig.EventBindingConfig> bindings;
    private final ProcessingFrequency freq;

    public Plc4xEventInput(String name,
                           Plc4xClientService svc,
                           List<Plc4xConfig.EventBindingConfig> bindings) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
        this.freq = AlarmSignal.PROCESSING_FREQUENCY;
    }

    @Override
    public List<IInputSignal> readSignals() {
        long now = System.currentTimeMillis();
        List<IInputSignal> out = new ArrayList<>();
        for (Plc4xConfig.EventBindingConfig b : bindings) {
            Plc4xDriver.ReadResponse resp = svc.latest(b.signalTag());
            if (resp == null) continue;
            out.addAll(Plc4xSignalMapper.decodeAlarmWord(b, resp, name, now));
        }
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return freq; }
}
