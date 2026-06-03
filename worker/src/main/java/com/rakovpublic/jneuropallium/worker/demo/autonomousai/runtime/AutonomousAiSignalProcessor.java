package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public class AutonomousAiSignalProcessor implements ISignalProcessor<AutonomousAiSignal, AutonomousAiNeuron> {
    public String signalProcessorClass = AutonomousAiSignalProcessor.class.getName();
    public String signalClassName = AutonomousAiSignal.class.getName();
    public String outputSignalClassName = AutonomousAiSignal.class.getName();
    public String stage = "stage";
    public String layerName = "layer";
    public String description = "autonomous AI gridworld processor";

    public AutonomousAiSignalProcessor() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(AutonomousAiSignal input, AutonomousAiNeuron neuron) {
        AutonomousAiSignal output = input.copySignal();
        output.setStage(stage);
        output.setLayerName(layerName);
        output.setNeuronLabel(neuron.getNeuronLabel());
        output.setResultType(stage);
        output.withAttribute("layer", layerName);
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
            throw new IllegalStateException("Cannot resolve autonomous AI processor class " + signalProcessorClass, e);
        }
    }

    @Override
    @JsonIgnore
    public Class<AutonomousAiNeuron> getNeuronClass() {
        return AutonomousAiNeuron.class;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<AutonomousAiSignal> getSignalClass() {
        try {
            return (Class<AutonomousAiSignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve autonomous AI signal class " + signalClassName, e);
        }
    }
}
