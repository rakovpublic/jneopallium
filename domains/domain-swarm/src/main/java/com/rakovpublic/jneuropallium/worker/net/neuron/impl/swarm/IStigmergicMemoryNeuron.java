package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PheromoneSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.StigmergicTraceSignal;

import java.util.List;

public interface IStigmergicMemoryNeuron extends IModulatableNeuron {
    void deposit(StigmergicTraceSignal trace);
    void deposit(PheromoneSignal p);
    /** Drop entries whose decay tick has passed. Returns the number evicted. */
    int evict(long currentTick);
    List<StigmergicTraceSignal> tracesNear(double[] origin, double radius);
    List<PheromoneSignal> pheromonesNear(double[] origin, double radius);
    int size();
}
