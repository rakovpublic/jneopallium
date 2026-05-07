/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.input.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaSignalMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Drains the alarm queue from {@link MiloOpcUaClientService} and emits
 * an {@link AlarmSignal} per delivered sample.
 */
public final class OpcUaAlarmInput implements IInitInput {

    private final String name;
    private final ProcessingFrequency freq;
    private final MiloOpcUaClientService svc;

    public OpcUaAlarmInput(String name, ProcessingFrequency freq, MiloOpcUaClientService svc) {
        this.name = Objects.requireNonNull(name);
        this.freq = freq != null ? freq : AlarmSignal.PROCESSING_FREQUENCY;
        this.svc = Objects.requireNonNull(svc);
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<MiloOpcUaClientService.AlarmEvent> drained = svc.drainAlarms();
        List<IInputSignal> out = new ArrayList<>(drained.size());
        for (MiloOpcUaClientService.AlarmEvent e : drained) {
            AlarmSignal s = OpcUaSignalMapper.toAlarm(e.binding(), e.value());
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
