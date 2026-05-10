/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains decoded EEG / Spikes / ECoG signals from
 * {@link LslClientService} (05-LSL.md §7 — {@code LslNeuralInput}).
 *
 * <p>One instance binds to one or more
 * {@link LslBridgeConfig.ReadSignalKind#LFP},
 * {@link LslBridgeConfig.ReadSignalKind#NEURAL_SPIKE},
 * {@link LslBridgeConfig.ReadSignalKind#ECOG} read bindings.
 * {@code readSignals()} is a snapshot — the platform service maintains the
 * cache asynchronously and never blocks on the LSL inlet's wire.
 */
public final class LslNeuralInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final LslClientService svc;
    private final List<String> bindingIds;

    public LslNeuralInput(String name, LslClientService svc, List<String> bindingIds) {
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
