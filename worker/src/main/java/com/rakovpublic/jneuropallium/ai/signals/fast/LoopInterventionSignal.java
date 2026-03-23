package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.enums.InterventionType;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class LoopInterventionSignal extends BaseSignal {
    private InterventionType type;
    private String targetId;
    private double parameter;
    private int durationTicks;

    public LoopInterventionSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public LoopInterventionSignal(InterventionType type, String targetId, double parameter, int durationTicks) {
        this(); this.type = type; this.targetId = targetId; this.parameter = parameter; this.durationTicks = durationTicks;
    }

    public InterventionType getType() { return type; }
    public void setType(InterventionType type) { this.type = type; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public double getParameter() { return parameter; }
    public void setParameter(double parameter) { this.parameter = parameter; }
    public int getDurationTicks() { return durationTicks; }
    public void setDurationTicks(int durationTicks) { this.durationTicks = durationTicks; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LoopInterventionSignal.class; }
    @Override public String getDescription() { return "LoopInterventionSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        LoopInterventionSignal c = new LoopInterventionSignal(type, targetId, parameter, durationTicks);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
