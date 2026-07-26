package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.enums.NeuromodulatorType;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class NeuromodulatorSignal extends BaseSignal {
    private NeuromodulatorType type;
    private double concentration;
    private String targetRegion;

    public NeuromodulatorSignal() { super(); this.loop = 2; this.epoch = 1L; }
    public NeuromodulatorSignal(NeuromodulatorType type, double concentration, String targetRegion) {
        this(); this.type = type; this.concentration = concentration; this.targetRegion = targetRegion;
    }

    public NeuromodulatorType getType() { return type; }
    public void setType(NeuromodulatorType type) { this.type = type; }
    public double getConcentration() { return concentration; }
    public void setConcentration(double concentration) { this.concentration = concentration; }
    public String getTargetRegion() { return targetRegion; }
    public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return NeuromodulatorSignal.class; }
    @Override public String getDescription() { return "NeuromodulatorSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        NeuromodulatorSignal c = new NeuromodulatorSignal(type, concentration, targetRegion);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
