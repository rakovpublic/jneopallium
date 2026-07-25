package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class AttentionGateSignal extends BaseSignal {
    private double salience;
    private String regionId;
    private boolean suppress;

    public AttentionGateSignal() { super(); this.loop = 1; this.epoch = 2L; }
    public AttentionGateSignal(double salience, String regionId, boolean suppress) {
        this(); this.salience = salience; this.regionId = regionId; this.suppress = suppress;
    }

    public double getSalience() { return salience; }
    public void setSalience(double salience) { this.salience = salience; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public boolean isSuppress() { return suppress; }
    public void setSuppress(boolean suppress) { this.suppress = suppress; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return AttentionGateSignal.class; }
    @Override public String getDescription() { return "AttentionGateSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        AttentionGateSignal c = new AttentionGateSignal(salience, regionId, suppress);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
