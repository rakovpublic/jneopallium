package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;

public class DemoInput implements IInitInput {
    public String demoId;
    public String name;
    public int ticks;
    public int cursor;
    public long epoch = 1L;
    public int loop = 1;

    public DemoInput() {
    }

    public DemoInput(String demoId, int ticks) {
        this.demoId = demoId;
        this.name = demoId + "-input";
        this.ticks = ticks;
    }

    @Override
    public List<IInputSignal> readSignals() {
        if (cursor >= ticks) {
            return List.of();
        }
        return DemoScenarioEngine.inputSignals(demoId, cursor++);
    }

    @Override
    public String getName() {
        return name == null ? demoId + "-input" : name;
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
