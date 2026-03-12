package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.enums.LoopType;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class LoopAlertSignal extends BaseSignal {
    private LoopType type;
    private String regionId;
    private String loopPath;
    private double severity;
    private int detectedAtTick;

    public LoopAlertSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public LoopAlertSignal(LoopType type, String regionId, String loopPath, double severity, int detectedAtTick) {
        this(); this.type = type; this.regionId = regionId; this.loopPath = loopPath;
        this.severity = severity; this.detectedAtTick = detectedAtTick;
    }

    public LoopType getType() { return type; }
    public void setType(LoopType type) { this.type = type; }
    public String getRegionId() { return regionId; }
    public void setRegionId(String regionId) { this.regionId = regionId; }
    public String getLoopPath() { return loopPath; }
    public void setLoopPath(String loopPath) { this.loopPath = loopPath; }
    public double getSeverity() { return severity; }
    public void setSeverity(double severity) { this.severity = severity; }
    public int getDetectedAtTick() { return detectedAtTick; }
    public void setDetectedAtTick(int detectedAtTick) { this.detectedAtTick = detectedAtTick; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return LoopAlertSignal.class; }
    @Override public String getDescription() { return "LoopAlertSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        LoopAlertSignal c = new LoopAlertSignal(type, regionId, loopPath, severity, detectedAtTick);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
