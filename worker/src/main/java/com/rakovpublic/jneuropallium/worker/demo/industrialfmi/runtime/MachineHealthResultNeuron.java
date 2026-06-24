package com.rakovpublic.jneuropallium.worker.demo.industrialfmi.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AdvisoryGateNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MachineHealthAdvisorySignal;

import java.util.Collections;
import java.util.List;

public class MachineHealthResultNeuron extends AdvisoryGateNeuron implements IResultNeuron<MachineHealthAdvisorySignal> {
    public MachineHealthResultNeuron() {
        super();
        currentNeuronClass = MachineHealthResultNeuron.class;
    }

    @Override
    public void setRun(Long run) {
        if (this.run == null || !this.run.equals(run)) {
            this.result.clear();
            this.signals.clear();
            this.isProcessed = false;
        }
        super.setRun(run);
    }

    @Override
    @JsonIgnore
    public MachineHealthAdvisorySignal getFinalResult() {
        MachineHealthAdvisorySignal best = null;
        for (ISignal signal : result) {
            if (signal instanceof MachineHealthAdvisorySignal advisory) {
                if (best == null || advisory.getAnomalyProbability() > best.getAnomalyProbability()) {
                    best = advisory;
                }
            }
        }
        if (best != null) {
            return best;
        }
        return new MachineHealthAdvisorySignal("UNKNOWN", "1.0.0-machine-health", "ADVISORY",
                1.0, 0.0, Collections.emptyMap(), 0.0, 0.0, 0.0,
                "MONITOR", List.of("no machine-health advisory generated in this tick"),
                false, 0L);
    }
}
