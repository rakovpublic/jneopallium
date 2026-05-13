/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains FHIR {@code Condition},
 * {@code DiagnosticReport}, and {@code Patient} bindings (06-FHIR.md §7
 * — {@code FhirConditionInput}). Confirmed conditions ride the same
 * channel as demographics because the differential-diagnosis neuron
 * consumes both as priors.
 */
public final class FhirConditionInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(10L, 2);

    private final String name;
    private final FhirClientService svc;
    private final List<String> bindingIds;
    private final boolean includeBridgeEvents;

    public FhirConditionInput(String name, FhirClientService svc,
                              List<String> bindingIds, boolean includeBridgeEvents) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
        this.bindingIds = List.copyOf(Objects.requireNonNull(bindingIds, "bindingIds"));
        this.includeBridgeEvents = includeBridgeEvents;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>();
        for (String b : bindingIds) out.addAll(svc.drain(b));
        if (includeBridgeEvents) out.addAll(svc.drainEvents());
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
