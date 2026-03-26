package com.rakovpublic.jneuropallium.ai.neurons.loop;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Region monitor neuron used by PassiveCountingProcessor.
 * Passively records spike magnitudes in a ring buffer and computes
 * mean, variance, and trend statistics for downstream loop detection.
 */
public class RegionMonitorNeuron extends ModulatableNeuron {

    private String monitoredRegion;
    private Deque<Double> firingHistory;
    private int ringBufferCapacity;

    public RegionMonitorNeuron() {
        super();
        this.monitoredRegion = "default";
        this.firingHistory = new ArrayDeque<>();
        this.ringBufferCapacity = 50;
    }

    public RegionMonitorNeuron(Long neuronId,
                               com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                               Long run,
                               String monitoredRegion,
                               int ringBufferCapacity) {
        super(neuronId, chain, run);
        this.monitoredRegion = monitoredRegion;
        this.firingHistory = new ArrayDeque<>();
        this.ringBufferCapacity = ringBufferCapacity;
    }

    public void recordFiring(double magnitude) {
        if (firingHistory.size() >= ringBufferCapacity) firingHistory.pollFirst();
        firingHistory.addLast(magnitude);
    }

    public double computeMean() {
        if (firingHistory.isEmpty()) return 0.0;
        double sum = 0;
        for (double v : firingHistory) sum += v;
        return sum / firingHistory.size();
    }

    public double computeVariance() {
        if (firingHistory.size() < 2) return 0.0;
        double mean = computeMean();
        double sumSq = 0;
        for (double v : firingHistory) sumSq += (v - mean) * (v - mean);
        return sumSq / firingHistory.size();
    }

    public double computeTrend() {
        if (firingHistory.size() < 2) return 0.0;
        Double[] arr = firingHistory.toArray(new Double[0]);
        int n = arr.length;
        return (arr[n - 1] - arr[0]) / (double) n;
    }

    public String getMonitoredRegion() { return monitoredRegion; }
    public void setMonitoredRegion(String monitoredRegion) { this.monitoredRegion = monitoredRegion; }

    public Deque<Double> getFiringHistory() { return firingHistory; }
    public void setFiringHistory(Deque<Double> firingHistory) { this.firingHistory = firingHistory; }

    public int getRingBufferCapacity() { return ringBufferCapacity; }
    public void setRingBufferCapacity(int ringBufferCapacity) { this.ringBufferCapacity = ringBufferCapacity; }
}
