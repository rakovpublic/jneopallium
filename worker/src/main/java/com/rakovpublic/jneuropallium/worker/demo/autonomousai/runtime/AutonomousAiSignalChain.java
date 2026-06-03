package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class AutonomousAiSignalChain implements ISignalChain {
    @Expose
    public String signalChainClass = AutonomousAiSignalChain.class.getName();
    @Expose
    public List<String> signalClassNames = new ArrayList<>();
    @Expose
    public String description = "autonomous AI gridworld signal chain";

    public AutonomousAiSignalChain() {
    }

    @Override
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<Class<? extends ISignal>> getProcessingChain() {
        List<Class<? extends ISignal>> result = new ArrayList<>();
        for (String signalClassName : signalClassNames) {
            try {
                result.add((Class<? extends ISignal>) Class.forName(signalClassName));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot resolve autonomous AI signal class " + signalClassName, e);
            }
        }
        return result;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
