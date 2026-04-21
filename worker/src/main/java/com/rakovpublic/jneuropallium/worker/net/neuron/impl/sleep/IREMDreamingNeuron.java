package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.sleep.DreamSignal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public interface IREMDreamingNeuron extends IModulatableNeuron {
    List<DreamSignal> recombine(SleepPhase phase, List<List<Long>> episodes);
    boolean isPlanningCandidate(DreamSignal dream);
    int getRecombinationCount();
    void setRecombinationCount(int v);
    double getMaxNoveltyForPlanning();
    void setMaxNoveltyForPlanning(double v);
}
