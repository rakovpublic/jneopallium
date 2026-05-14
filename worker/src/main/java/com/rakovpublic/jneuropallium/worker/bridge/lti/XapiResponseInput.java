/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains xAPI response bindings
 * (14-LTI-XAPI.md §7 — {@code XapiResponseInput}). Each configured
 * binding emits {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal}s
 * or {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal}s
 * decoded by {@link XapiStatementMapper} inside {@link XapiClientService}.
 */
public final class XapiResponseInput implements IInitInput {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final XapiClientService svc;
    private final List<String> bindingIds;

    public XapiResponseInput(String name, XapiClientService svc, List<String> bindingIds) {
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
