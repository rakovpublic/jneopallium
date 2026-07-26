package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class DemoSignalChain implements ISignalChain {
    @Expose
    public String signalChainClass = DemoSignalChain.class.getName();
    @Expose
    public List<String> signalClassNames = new ArrayList<>();
    @Expose
    public String description = "full-run demo signal chain";

    public DemoSignalChain() {
    }

    public DemoSignalChain(List<String> signalClassNames, String description) {
        this.signalClassNames = new ArrayList<>(signalClassNames);
        this.description = description;
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
                throw new IllegalStateException("Cannot resolve signal class " + signalClassName, e);
            }
        }
        return result;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
