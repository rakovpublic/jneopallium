/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Layer 0 rolling-window trend detector per {@link VitalType}. Uses
 * least-squares slope over a fixed-size window. Emits {@link Trend#UP},
 * {@link Trend#DOWN}, or {@link Trend#FLAT} based on a configurable slope
 * threshold. Biological analogue: slow adapting peripheral receptor arrays
 * that report rate-of-change rather than absolute value. Loop=1 / Epoch=2.
 */
public class TrendDetectorNeuron extends ModulatableNeuron implements ITrendDetectorNeuron {

    public enum Trend { UP, DOWN, FLAT }

    private final Map<VitalType, Deque<double[]>> windows = new EnumMap<>(VitalType.class);
    private int windowSize = 16;
    private double slopeUpThreshold = 0.01;

    public TrendDetectorNeuron() { super(); }

    public TrendDetectorNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    public void setWindowSize(int n) { this.windowSize = Math.max(2, n); }
    public int getWindowSize() { return windowSize; }
    public void setSlopeUpThreshold(double v) { this.slopeUpThreshold = Math.max(0.0, v); }
    public double getSlopeUpThreshold() { return slopeUpThreshold; }

    public Trend observe(VitalSignal v) {
        if (v == null) return Trend.FLAT;
        Deque<double[]> w = windows.computeIfAbsent(v.getType(), k -> new ArrayDeque<>());
        w.addLast(new double[]{v.getTimestamp(), v.getMeasurement()});
        while (w.size() > windowSize) w.removeFirst();
        if (w.size() < 3) return Trend.FLAT;
        double slope = leastSquaresSlope(w);
        if (slope > slopeUpThreshold) return Trend.UP;
        if (slope < -slopeUpThreshold) return Trend.DOWN;
        return Trend.FLAT;
    }

    private double leastSquaresSlope(Deque<double[]> w) {
        int n = w.size();
        double sx = 0, sy = 0, sxx = 0, sxy = 0;
        int i = 0;
        double x0 = w.peekFirst()[0];
        for (double[] p : w) {
            double x = p[0] - x0;
            double y = p[1];
            sx += x; sy += y; sxx += x * x; sxy += x * y;
            i++;
        }
        double denom = n * sxx - sx * sx;
        if (denom == 0) return 0.0;
        return (n * sxy - sx * sy) / denom;
    }

    public int samplesFor(VitalType t) {
        Deque<double[]> w = windows.get(t);
        return w == null ? 0 : w.size();
    }

    public void reset() { windows.clear(); }
}
