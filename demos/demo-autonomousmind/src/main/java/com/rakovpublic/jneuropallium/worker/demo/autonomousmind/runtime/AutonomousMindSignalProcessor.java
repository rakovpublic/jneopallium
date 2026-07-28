package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public class AutonomousMindSignalProcessor implements ISignalProcessor<AutonomousMindSignal, AutonomousMindNeuron> {
    public String signalProcessorClass = AutonomousMindSignalProcessor.class.getName();
    public String signalClassName = AutonomousMindSignal.class.getName();
    public String outputSignalClassName = AutonomousMindSignal.class.getName();
    public String cognitiveSystem = "system";
    public String stage = "system";
    public String layerName = "system";
    public String description = "AutonomousMind processor";

    public AutonomousMindSignalProcessor() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AutonomousMindSignal input, AutonomousMindNeuron neuron) {
        AutonomousMindSignal output = input.copySignal();
        String systemName = layerName == null || "system".equals(layerName) ? cognitiveSystem : layerName;
        output.setSystem(systemName);
        output.setSignalType(stage + "Signal");
        output.withAttribute("cognitiveSystem", systemName);
        output.withAttribute("stage", stage);
        output.withAttribute("neuron", neuron.getNeuronLabel());
        return (List<I>) List.of(output);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        try {
            return (Class<? extends ISignalProcessor>) Class.forName(signalProcessorClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve AutonomousMind processor " + signalProcessorClass, e);
        }
    }

    @Override
    @JsonIgnore
    public Class<AutonomousMindNeuron> getNeuronClass() {
        return AutonomousMindNeuron.class;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<AutonomousMindSignal> getSignalClass() {
        try {
            return (Class<AutonomousMindSignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve AutonomousMind signal " + signalClassName, e);
        }
    }
}
