/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.swarm;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.PheromoneSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.swarm.StigmergicTraceSignal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Layer 3 spatial cache of pheromone + trace markers. Loop=2 / Epoch=5.
 */
public class StigmergicMemoryNeuron extends ModulatableNeuron implements IStigmergicMemoryNeuron {

    private final List<StigmergicTraceSignal> traces = new ArrayList<>();
    private final List<PheromoneSignal> pheromones = new ArrayList<>();

    public StigmergicMemoryNeuron() { super(); }
    public StigmergicMemoryNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void deposit(StigmergicTraceSignal t) { if (t != null) traces.add(t); }
    @Override public void deposit(PheromoneSignal p) { if (p != null) pheromones.add(p); }

    @Override
    public int evict(long currentTick) {
        int n = 0;
        Iterator<PheromoneSignal> it = pheromones.iterator();
        while (it.hasNext()) {
            PheromoneSignal p = it.next();
            if (p.getDecayTick() > 0 && p.getDecayTick() <= currentTick) { it.remove(); n++; }
        }
        return n;
    }

    @Override
    public List<StigmergicTraceSignal> tracesNear(double[] origin, double radius) {
        List<StigmergicTraceSignal> out = new ArrayList<>();
        if (origin == null) return out;
        double r2 = radius * radius;
        for (StigmergicTraceSignal t : traces) {
            if (within(t.getLocationGlobal(), origin, r2)) out.add(t);
        }
        return out;
    }

    @Override
    public List<PheromoneSignal> pheromonesNear(double[] origin, double radius) {
        List<PheromoneSignal> out = new ArrayList<>();
        if (origin == null) return out;
        double r2 = radius * radius;
        for (PheromoneSignal p : pheromones) {
            if (within(p.getLocationGlobal(), origin, r2)) out.add(p);
        }
        return out;
    }

    @Override public int size() { return traces.size() + pheromones.size(); }

    private static boolean within(double[] a, double[] b, double r2) {
        if (a == null || b == null) return false;
        int n = Math.min(a.length, b.length);
        double sum = 0.0;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; sum += d * d; }
        return sum <= r2;
    }
}
