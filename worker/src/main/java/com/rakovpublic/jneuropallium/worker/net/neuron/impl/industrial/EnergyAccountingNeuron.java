/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.HashMap;
import java.util.Map;

/**
 * Layer 7 production-aware energy accounting. Efficiency is reported as
 * production-units per kWh for each unit against a baseline the first
 * observation establishes. Loop=2 / Epoch=1.
 */
public class EnergyAccountingNeuron extends ModulatableNeuron implements IEnergyAccountingNeuron {

    private static final class Unit { double kwh; double production; double baseline = Double.NaN; }

    private final Map<String, Unit> units = new HashMap<>();

    public EnergyAccountingNeuron() { super(); }
    public EnergyAccountingNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void recordEnergy(String unitId, double kwh) {
        if (unitId == null) return;
        units.computeIfAbsent(unitId, k -> new Unit()).kwh += Math.max(0.0, kwh);
    }

    @Override public void recordProduction(String unitId, double production) {
        if (unitId == null) return;
        units.computeIfAbsent(unitId, k -> new Unit()).production += Math.max(0.0, production);
    }

    @Override
    public EfficiencySignal efficiencyFor(String unitId) {
        Unit u = units.get(unitId);
        if (u == null || u.kwh <= 0.0) return new EfficiencySignal(unitId, 0.0, 0.0);
        double eff = u.production / u.kwh;
        if (Double.isNaN(u.baseline)) u.baseline = eff;
        return new EfficiencySignal(unitId, eff, u.baseline);
    }

    @Override
    public void observe(MeasurementSignal m) {
        if (m == null || m.getTag() == null) return;
        // Convention: tag format is "<unitId>/energy" or "<unitId>/production".
        int slash = m.getTag().lastIndexOf('/');
        if (slash <= 0) return;
        String unit = m.getTag().substring(0, slash);
        String kind = m.getTag().substring(slash + 1);
        if ("energy".equalsIgnoreCase(kind)) recordEnergy(unit, m.getMeasurement());
        else if ("production".equalsIgnoreCase(kind)) recordProduction(unit, m.getMeasurement());
    }
}
