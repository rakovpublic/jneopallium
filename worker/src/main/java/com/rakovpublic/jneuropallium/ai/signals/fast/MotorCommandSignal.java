package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class MotorCommandSignal extends BaseSignal {
    private int effectorId;
    private double[] params;
    private boolean execute = false; // MUST default to false
    private String actionPlanId;

    public MotorCommandSignal() { super(); this.loop = 1; this.epoch = 1L; this.execute = false; }
    public MotorCommandSignal(int effectorId, double[] params) {
        this(); this.effectorId = effectorId; this.params = params; this.execute = false;
    }

    public int getEffectorId() { return effectorId; }
    public void setEffectorId(int effectorId) { this.effectorId = effectorId; }
    public double[] getParams() { return params; }
    public void setParams(double[] params) { this.params = params; }
    public boolean isExecute() { return execute; }
    /** Only HarmGateNeuron should call this with true. */
    public void setExecute(boolean execute) { this.execute = execute; }
    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return MotorCommandSignal.class; }
    @Override public String getDescription() { return "MotorCommandSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        MotorCommandSignal c = new MotorCommandSignal(effectorId, params);
        c.execute = false; // copies preserve execute=false for safety
        c.actionPlanId = this.actionPlanId;
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
