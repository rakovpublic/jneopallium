package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class HomeostasisSignal extends BaseSignal {
    private double currentActivity;
    private double targetActivity;
    private String regionId;

    public HomeostasisSignal() { super(); this.loop = 2; this.epoch = 2L; }
    public HomeostasisSignal(double currentActivity, double targetActivity, String regionId) {
        this(); this.currentActivity = currentActivity; this.targetActivity = targetActivity; this.regionId = regionId;
    }

    public double getCurrentActivity() { return currentActivity; }
    public void setCurrentActivity(double currentActivity) { this.currentActivity = currentActivity; }
    public double getTargetActivity() { return targetActivity; }
    public void setTargetActivity(double targetActivity) { this.targetActivity = targetActivity; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HomeostasisSignal.class; }
    @Override public String getDescription() { return "HomeostasisSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        HomeostasisSignal c = new HomeostasisSignal(currentActivity, targetActivity, regionId);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
