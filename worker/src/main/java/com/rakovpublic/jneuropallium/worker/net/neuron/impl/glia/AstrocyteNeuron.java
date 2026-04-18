/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.CalciumWaveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.glia.GliotransmitterSignal;

/**
 * Support neuron that integrates local activity and emits both a
 * {@link CalciumWaveSignal} (when activity exceeds the calcium-wave
 * threshold) and a {@link GliotransmitterSignal} regulating local
 * excitability.
 * Support layer; loop=2 / epoch=1.
 * <p>Biological analogue: protoplasmic astrocyte at the tripartite
 * synapse (Volterra &amp; Meldolesi 2005).
 */
public class AstrocyteNeuron extends ModulatableNeuron {

    private final int regionId;
    private double integratedActivity;
    private double calciumWaveThreshold = 0.4;
    private double propagationRadius = 1.0;

    public AstrocyteNeuron() { this(0); }

    public AstrocyteNeuron(int regionId) {
        super();
        this.regionId = regionId;
    }

    public AstrocyteNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
        this.regionId = 0;
    }

    public void accumulate(double activityDelta) {
        integratedActivity = Math.max(0.0, integratedActivity + activityDelta);
    }

    /**
     * Check the integrated activity and, if it exceeds threshold, emit a
     * calcium wave. Clears the integrator as a side-effect of emission.
     */
    public CalciumWaveSignal maybeEmitWave() {
        if (integratedActivity < calciumWaveThreshold) return null;
        CalciumWaveSignal s = new CalciumWaveSignal(regionId, integratedActivity, propagationRadius);
        s.setSourceNeuronId(this.getId());
        integratedActivity = 0.0;
        return s;
    }

    public GliotransmitterSignal release(GliotransmitterType t, double concentration) {
        GliotransmitterSignal s = new GliotransmitterSignal(t, concentration, regionId);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public int getRegionId() { return regionId; }
    public double getIntegratedActivity() { return integratedActivity; }
    public double getCalciumWaveThreshold() { return calciumWaveThreshold; }
    public void setCalciumWaveThreshold(double v) { this.calciumWaveThreshold = Math.max(0.0, v); }
    public double getPropagationRadius() { return propagationRadius; }
    public void setPropagationRadius(double v) { this.propagationRadius = Math.max(0.0, v); }
}
