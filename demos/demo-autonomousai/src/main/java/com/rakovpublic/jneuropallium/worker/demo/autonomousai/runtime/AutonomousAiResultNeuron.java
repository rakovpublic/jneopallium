package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AutonomousAiResultNeuron extends AutonomousAiNeuron implements IResultNeuron<AutonomousAiSignal> {
    public AutonomousAiResultNeuron() {
        super();
        currentNeuronClass = AutonomousAiResultNeuron.class;
    }

    @Override
    @JsonIgnore
    public AutonomousAiSignal getFinalResult() {
        AutonomousAiSignal fallback = null;
        for (ISignal signal : result) {
            if (signal instanceof AutonomousAiSignal autonomousSignal) {
                fallback = autonomousSignal;
                if (autonomousSignal.getResultType() != null && !autonomousSignal.getResultType().isBlank()) {
                    return autonomousSignal;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        AutonomousAiSignal empty = new AutonomousAiSignal();
        empty.setSignalType("NO_RESULT");
        empty.setResultType("NO_RESULT");
        empty.setStage("result-output");
        return empty;
    }
}
