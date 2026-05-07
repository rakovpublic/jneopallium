/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.input.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaSignalMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Snapshot-based {@link IInitInput} that emits {@link MeasurementSignal}s
 * for the configured read bindings. The heavy lifting (subscriptions,
 * latest-value cache) lives in {@link MiloOpcUaClientService}.
 */
public final class OpcUaMeasurementInput implements IInitInput {

    private final String name;
    private final ProcessingFrequency freq;
    private final MiloOpcUaClientService svc;
    private final List<OpcUaNodeBinding> bindings;

    public OpcUaMeasurementInput(String name,
                                 ProcessingFrequency freq,
                                 MiloOpcUaClientService svc,
                                 List<OpcUaNodeBinding> bindings) {
        this.name = Objects.requireNonNull(name);
        this.freq = freq != null ? freq : MeasurementSignal.PROCESSING_FREQUENCY;
        this.svc = Objects.requireNonNull(svc);
        this.bindings = List.copyOf(bindings);
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> out = new ArrayList<>(bindings.size());
        for (OpcUaNodeBinding b : bindings) {
            DataValue dv = svc.latest(b.signalTag);
            if (dv == null) continue;
            MeasurementSignal s = OpcUaSignalMapper.toMeasurement(b, dv);
            s.setInputName(name);
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
