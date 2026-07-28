/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.InterlockSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Layer 5 hard-wired interlocks. Rules are frozen at {@link #seal()};
 * any subsequent addition throws — the SRS is compiled once. Per spec
 * §5 the interlock is the only signal path permitted to bypass planning
 * and action-selection, and only in the fail-safe direction.
 * Loop=1 / Epoch=1.
 */
public class InterlockNeuron extends ModulatableNeuron implements IInterlockNeuron {

    private static final class Rule {
        final String id;
        final String measTag;
        final double threshold;
        final boolean tripHigh;
        final String safeActuator;
        final double safeValue;
        Rule(String id, String t, double th, boolean high, String sa, double sv) {
            this.id = id; this.measTag = t; this.threshold = th;
            this.tripHigh = high; this.safeActuator = sa; this.safeValue = sv;
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private final List<ActuatorCommandSignal> lastFailSafe = new ArrayList<>();
    private boolean sealed;

    public InterlockNeuron() { super(); }
    public InterlockNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void addInterlock(String interlockId, String measurementTag, double threshold,
                             boolean tripHigh, String safeActuatorTag, double safeValue) {
        if (sealed) throw new IllegalStateException("interlock set is sealed");
        if (interlockId == null || measurementTag == null || safeActuatorTag == null) return;
        rules.add(new Rule(interlockId, measurementTag, threshold, tripHigh, safeActuatorTag, safeValue));
    }

    @Override public void seal() { this.sealed = true; }
    @Override public boolean isSealed() { return sealed; }

    @Override
    public List<InterlockSignal> evaluate(MeasurementSignal m) {
        List<InterlockSignal> out = new ArrayList<>();
        lastFailSafe.clear();
        if (m == null || m.getTag() == null) return out;
        for (Rule r : rules) {
            if (!r.measTag.equals(m.getTag())) continue;
            boolean trip = r.tripHigh ? m.getMeasurement() > r.threshold
                                       : m.getMeasurement() < r.threshold;
            if (trip) {
                out.add(new InterlockSignal(r.id, true, Arrays.asList(r.measTag)));
                lastFailSafe.add(new ActuatorCommandSignal(r.safeActuator, r.safeValue, Double.NaN, true));
            }
        }
        return out;
    }

    @Override public List<ActuatorCommandSignal> failSafeCommands() {
        return new ArrayList<>(lastFailSafe);
    }
}
