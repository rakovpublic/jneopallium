package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ConsolidationSignal extends BaseSignal {
    private String memoryId;
    private double importance;
    private boolean promote;

    public ConsolidationSignal() { super(); this.loop = 2; this.epoch = 3L; }
    public ConsolidationSignal(String memoryId, double importance, boolean promote) {
        this(); this.memoryId = memoryId; this.importance = importance; this.promote = promote;
    }

    public String getMemoryId() { return memoryId; }
    public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
    public boolean isPromote() { return promote; }
    public void setPromote(boolean promote) { this.promote = promote; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ConsolidationSignal.class; }
    @Override public String getDescription() { return "ConsolidationSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ConsolidationSignal c = new ConsolidationSignal(memoryId, importance, promote);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
