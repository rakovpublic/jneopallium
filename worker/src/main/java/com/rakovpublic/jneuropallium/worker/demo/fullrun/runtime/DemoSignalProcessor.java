package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class DemoSignalProcessor implements ISignalProcessor<DemoSignal, DemoNeuron> {
    public String signalProcessorClass = DemoSignalProcessor.class.getName();
    public String signalClassName = DemoSignal.class.getName();
    public String outputSignalClassName = DemoSignal.class.getName();
    public String stage = "stage";
    public String description = "full-run demo processor";

    public DemoSignalProcessor() {
    }

    public DemoSignalProcessor(String signalClassName, String outputSignalClassName, String stage, String description) {
        this.signalClassName = signalClassName;
        this.outputSignalClassName = outputSignalClassName;
        this.stage = stage;
        this.description = description;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(DemoSignal input, DemoNeuron neuron) {
        DemoSignal output = newOutputSignal();
        input.copyInto(output);
        output.setSignalType(stage);
        DemoScenarioEngine.applyStage(neuron.getDemoId(), stage, input, output);
        return (List<I>) List.of(output);
    }

    protected DemoSignal newOutputSignal() {
        try {
            return (DemoSignal) Class.forName(outputSignalClassName).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException | ClassNotFoundException e) {
            throw new IllegalStateException("Cannot instantiate output signal " + outputSignalClassName, e);
        }
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
            throw new IllegalStateException("Cannot resolve processor class " + signalProcessorClass, e);
        }
    }

    @Override
    @JsonIgnore
    public Class<DemoNeuron> getNeuronClass() {
        return DemoNeuron.class;
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public Class<DemoSignal> getSignalClass() {
        try {
            return (Class<DemoSignal>) Class.forName(signalClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot resolve signal class " + signalClassName, e);
        }
    }
}
