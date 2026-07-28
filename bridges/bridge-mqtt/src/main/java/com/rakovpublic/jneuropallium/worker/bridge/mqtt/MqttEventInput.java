/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that drains {@link AlarmSignal}s emitted by
 * {@link MqttClientService} from {@code DDEATH/NDEATH} or {@code BRIDGE_RECONNECTED}
 * conditions (02-MQTT-SPARKPLUG.md §5).
 */
public final class MqttEventInput implements IInitInput {

    private final String name;
    private final MqttClientService svc;

    public MqttEventInput(String name, MqttClientService svc) {
        this.name = Objects.requireNonNull(name, "name");
        this.svc = Objects.requireNonNull(svc, "svc");
    }

    @Override public List<IInputSignal> readSignals() { return svc.drainEvents(); }
    @Override public String getName() { return name; }
    @Override public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }
    @Override public ProcessingFrequency getDefaultProcessingFrequency() {
        return AlarmSignal.PROCESSING_FREQUENCY;
    }
}
