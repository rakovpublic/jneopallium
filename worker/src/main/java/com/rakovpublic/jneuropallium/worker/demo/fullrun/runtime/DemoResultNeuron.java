package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DemoResultNeuron extends DemoNeuron implements IResultNeuron<DemoSignal> {
    public DemoResultNeuron() {
        super();
        currentNeuronClass = DemoResultNeuron.class;
    }

    @Override
    @JsonIgnore
    public DemoSignal getFinalResult() {
        DemoSignal fallback = null;
        for (ISignal signal : result) {
            if (signal instanceof DemoSignal demoSignal) {
                fallback = demoSignal;
                if (demoSignal.getResultType() != null && !demoSignal.getResultType().isBlank()) {
                    return demoSignal;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        DemoSignal empty = new DemoSignal();
        empty.setDemoId(demoId);
        empty.setSignalType("empty-result");
        empty.setResultType("NO_RESULT");
        empty.setDecision("NO_RESULT");
        empty.setConfidence(0.0);
        return empty;
    }
}
