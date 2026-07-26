/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

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
 * Snapshot-based {@link IInitInput} that emits one {@link MeasurementSignal}
 * per configured PLC4X read binding (01-PLC4X.md §4, signal table row 1).
 *
 * <p>Reads from the {@link Plc4xClientService} latest cache populated by the
 * per-connection polling loops. Bindings whose latest read has never returned
 * (cache miss) are skipped — they will appear once the polling thread has
 * had a chance to run.
 *
 * <p>Per 00-FRAMEWORK §0.5, signals carry the protocol-derived
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality}
 * unmodified — including BAD/UNCERTAIN values. Downstream neurons decide
 * what to do with low-quality data.
 */
public final class Plc4xMeasurementInput implements IInitInput {

    private final String name;
    private final Plc4xClientService svc;
    private final List<Plc4xConfig.ReadBindingConfig> bindings;
    private final ProcessingFrequency freq;

    public Plc4xMeasurementInput(String name,
                                 Plc4xClientService svc,
                                 List<Plc4xConfig.ReadBindingConfig> bindings) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
        this.freq = MeasurementSignal.PROCESSING_FREQUENCY;
    }

    @Override
    public List<IInputSignal> readSignals() {
        long now = System.currentTimeMillis();
        List<IInputSignal> out = new ArrayList<>(bindings.size());
        for (Plc4xConfig.ReadBindingConfig b : bindings) {
            Plc4xDriver.ReadResponse resp = svc.latest(b.signalTag());
            if (resp == null) continue;   // never polled yet
            out.add(Plc4xSignalMapper.toMeasurement(b, resp, name, now));
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
