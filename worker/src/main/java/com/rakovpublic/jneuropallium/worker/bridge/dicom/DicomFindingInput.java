/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains DICOM bindings (07-DICOM.md §7 —
 * {@code DicomFindingInput}). Each configured binding's {@code targetSignal}
 * ({@code IMAGING_FINDING}) is decoded by {@link DicomSignalMapper} inside
 * {@link DicomClientService}; this adapter only walks the binding-id list
 * and snapshots their queues.
 */
public final class DicomFindingInput implements IInitInput {

    /**
     * Matches {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal#PROCESSING_FREQUENCY}
     * (loop=2, epoch=5) so the bridge pace lines up with the consumer
     * neuron's processing cadence.
     */
    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(5L, 2);

    private final String name;
    private final DicomClientService svc;
    private final List<String> bindingIds;

    public DicomFindingInput(String name, DicomClientService svc, List<String> bindingIds) {
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
