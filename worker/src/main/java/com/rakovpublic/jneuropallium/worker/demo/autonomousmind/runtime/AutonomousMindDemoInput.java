package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;

public class AutonomousMindDemoInput implements IInitInput {
    public String scenarioId;
    public String scenarioPath;
    public String outputDir;
    public String name;
    public int ticks;
    public int cursor;
    public long seed;
    public long epoch = 1L;
    public int loop = 1;

    public AutonomousMindDemoInput() {
    }

    @Override
    public List<IInputSignal> readSignals() {
        if (cursor >= ticks) {
            return List.of();
        }
        return AutonomousMindSimulation.inputSignals(this, cursor++);
    }

    @Override
    public String getName() {
        return name == null || name.isBlank() ? scenarioId + "-input" : name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() {
        return new ProcessingFrequency(epoch, loop);
    }
}
