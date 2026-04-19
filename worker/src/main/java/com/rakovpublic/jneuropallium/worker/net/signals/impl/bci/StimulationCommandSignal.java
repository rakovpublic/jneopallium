/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.bci;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.PolarityPattern;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Request to emit a stimulation pulse train on one electrode. Must pass the
 * safety gate (Shannon criterion, charge balance, frequency SOA) before
 * reaching the hardware.
 * ProcessingFrequency: loop=1, epoch=1.
 */
public class StimulationCommandSignal extends AbstractSignal<Void> implements ISignal<Void> {

    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private int electrodeId;
    private double amplitudeUA;
    private double pulseWidthUS;
    private double frequencyHz;
    private int nPulses;
    private PolarityPattern pattern;

    public StimulationCommandSignal() {
        super();
        this.loop = 1;
        this.epoch = 1L;
        this.timeAlive = 5;
        this.pattern = PolarityPattern.CATHODIC_FIRST_BIPHASIC;
    }

    public StimulationCommandSignal(int electrodeId, double amplitudeUA, double pulseWidthUS,
                                    double frequencyHz, int nPulses, PolarityPattern pattern) {
        this();
        this.electrodeId = electrodeId;
        this.amplitudeUA = amplitudeUA;
        this.pulseWidthUS = pulseWidthUS;
        this.frequencyHz = frequencyHz;
        this.nPulses = nPulses;
        this.pattern = pattern == null ? PolarityPattern.CATHODIC_FIRST_BIPHASIC : pattern;
    }

    public int getElectrodeId() { return electrodeId; }
    public void setElectrodeId(int e) { this.electrodeId = e; }
    public double getAmplitudeUA() { return amplitudeUA; }
    public void setAmplitudeUA(double a) { this.amplitudeUA = a; }
    public double getPulseWidthUS() { return pulseWidthUS; }
    public void setPulseWidthUS(double p) { this.pulseWidthUS = p; }
    public double getFrequencyHz() { return frequencyHz; }
    public void setFrequencyHz(double f) { this.frequencyHz = f; }
    public int getNPulses() { return nPulses; }
    public void setNPulses(int n) { this.nPulses = n; }
    public PolarityPattern getPattern() { return pattern; }
    public void setPattern(PolarityPattern p) { this.pattern = p == null ? PolarityPattern.CATHODIC_FIRST_BIPHASIC : p; }

    /** Charge per phase in microcoulombs, assuming constant-current rectangular pulse. */
    public double chargePerPhaseUC() {
        return (amplitudeUA * pulseWidthUS) / 1_000_000.0;
    }

    @Override public Void getValue() { return null; }
    @Override public Class<Void> getParamClass() { return Void.class; }
    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return StimulationCommandSignal.class; }
    @Override public String getDescription() { return "StimulationCommandSignal"; }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends ISignal<Void>> K copySignal() {
        StimulationCommandSignal c = new StimulationCommandSignal(electrodeId, amplitudeUA, pulseWidthUS,
                frequencyHz, nPulses, pattern);
        c.sourceLayer = this.sourceLayer;
        c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop;
        c.epoch = this.epoch;
        c.name = this.name;
        return (K) c;
    }
}
