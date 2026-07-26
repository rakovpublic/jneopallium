package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AutonomousMindResultNeuron extends AutonomousMindNeuron implements IResultNeuron<AutonomousMindSignal> {
    public AutonomousMindResultNeuron() {
        super();
        currentNeuronClass = AutonomousMindResultNeuron.class;
    }

    @Override
    @JsonIgnore
    public AutonomousMindSignal getFinalResult() {
        AutonomousMindSignal fallback = null;
        for (ISignal signal : result) {
            if (signal instanceof AutonomousMindSignal mindSignal) {
                fallback = mindSignal;
                if (mindSignal.getSignalType() != null && !mindSignal.getSignalType().isBlank()) {
                    return mindSignal;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        AutonomousMindSignal empty = new AutonomousMindSignal();
        empty.setSignalType("NO_RESULT");
        empty.setSystem("result-output");
        return empty;
    }
}
