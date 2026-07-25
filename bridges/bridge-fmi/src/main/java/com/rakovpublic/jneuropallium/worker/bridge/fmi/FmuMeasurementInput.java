/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

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
 * per configured FMU Real output (03-FMI-FMU.md §5, signal table row 1).
 *
 * <p>Reads from the {@link FmuClientService} cache populated by the previous
 * {@link FmuClientService#step}. Signals that have not yet been read from the
 * FMU (cache returns {@code Double.NaN}) are skipped so the pipeline never
 * sees a NaN measurement.
 */
public final class FmuMeasurementInput implements IInitInput {

    private final String name;
    private final FmuClientService svc;
    private final List<FmiBridgeConfig.ReadBindingConfig> bindings;
    private final ProcessingFrequency freq;

    public FmuMeasurementInput(String name,
                               FmuClientService svc,
                               List<FmiBridgeConfig.ReadBindingConfig> bindings) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
        this.freq = MeasurementSignal.PROCESSING_FREQUENCY;
    }

    @Override
    public List<IInputSignal> readSignals() {
        long now = System.currentTimeMillis();
        List<IInputSignal> out = new ArrayList<>(bindings.size());
        for (FmiBridgeConfig.ReadBindingConfig b : bindings) {
            double value = svc.getReal(b.fmuVariable());
            if (Double.isNaN(value)) continue;
            MeasurementSignal s = FmuSignalMapper.toMeasurement(b, value, name, now);
            out.add(s);
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
