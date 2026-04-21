package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.DreamSignal;

import java.util.List;

public interface IREMDreamingNeuron extends IModulatableNeuron {
    List<DreamSignal> recombine(SleepPhase phase, List<List<Long>> episodes);
    boolean isPlanningCandidate(DreamSignal dream);
    int getRecombinationCount();
    void setRecombinationCount(int v);
    double getMaxNoveltyForPlanning();
    void setMaxNoveltyForPlanning(double v);
}
