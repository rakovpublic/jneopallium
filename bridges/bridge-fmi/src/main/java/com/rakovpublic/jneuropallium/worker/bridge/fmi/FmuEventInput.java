/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

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
 * Snapshot-based {@link IInitInput} that emits {@link AlarmSignal}s for
 * FMU Boolean outputs whose alarm flag is currently {@code true}
 * (03-FMI-FMU.md §5, signal table row 2).
 *
 * <p>Only active (true) alarms are emitted; cleared alarms produce no signal.
 * Severity is taken from {@link FmiBridgeConfig.EventBindingConfig#severity()}
 * and mapped to {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority}
 * by {@link FmuSignalMapper}.
 */
public final class FmuEventInput implements IInitInput {

    private final String name;
    private final FmuClientService svc;
    private final List<FmiBridgeConfig.EventBindingConfig> bindings;
    private final ProcessingFrequency freq;

    public FmuEventInput(String name,
                         FmuClientService svc,
                         List<FmiBridgeConfig.EventBindingConfig> bindings) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
        this.freq = AlarmSignal.PROCESSING_FREQUENCY;
    }

    @Override
    public List<IInputSignal> readSignals() {
        long now = System.currentTimeMillis();
        List<IInputSignal> out = new ArrayList<>();
        for (FmiBridgeConfig.EventBindingConfig b : bindings) {
            boolean active = svc.getBoolean(b.fmuVariable());
            AlarmSignal s = FmuSignalMapper.toAlarm(b, active, name, now);
            if (s != null) out.add(s);
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
