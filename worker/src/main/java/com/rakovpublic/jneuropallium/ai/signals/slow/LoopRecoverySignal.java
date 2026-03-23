package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class LoopRecoverySignal extends BaseSignal {
    private String regionId;
    private String interventionId;
    private boolean restored;

    public LoopRecoverySignal() { super(); this.loop = 2; this.epoch = 1L; }
    public LoopRecoverySignal(String regionId, String interventionId, boolean restored) {
        this(); this.regionId = regionId; this.interventionId = interventionId; this.restored = restored;
    }

    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public String getInterventionId() { return interventionId; }
    public void setInterventionId(String interventionId) { this.interventionId = interventionId; }
    public boolean isRestored() { return restored; }
    public void setRestored(boolean restored) { this.restored = restored; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LoopRecoverySignal.class; }
    @Override public String getDescription() { return "LoopRecoverySignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        LoopRecoverySignal c = new LoopRecoverySignal(regionId, interventionId, restored);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
