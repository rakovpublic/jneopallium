package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ErrorSignal extends BaseSignal {
    private double delta;
    private String targetNeuronId;
    private boolean isBackward;

    public ErrorSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public ErrorSignal(double delta, String targetNeuronId, boolean isBackward) {
        this(); this.delta = delta; this.targetNeuronId = targetNeuronId; this.isBackward = isBackward;
    }

    public double getDelta() { return delta; }
    public void setDelta(double delta) { this.delta = delta; }
    public String getTargetNeuronId() { return targetNeuronId; }
    public void setTargetNeuronId(String targetNeuronId) { this.targetNeuronId = targetNeuronId; }
    public boolean isBackward() { return isBackward; }
    public void setBackward(boolean backward) { isBackward = backward; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ErrorSignal.class; }
    @Override public String getDescription() { return "ErrorSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ErrorSignal c = new ErrorSignal(delta, targetNeuronId, isBackward);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
