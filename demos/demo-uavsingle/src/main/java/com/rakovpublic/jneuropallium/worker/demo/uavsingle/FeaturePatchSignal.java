package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.Arrays;

public class FeaturePatchSignal extends UavSingleSignal {
    private String frameId;
    private String sourceLayerName;
    private String sourceFeatureName;
    private String targetLayerName;
    private int patchX;
    private int patchY;
    private double[] activations = new double[9];

    public FeaturePatchSignal() {
        setEventType("FEATURE_PATCH_3X3");
    }

    public FeaturePatchSignal(String missionId, String uavId, long tick, String frameId,
                              String sourceLayerName, String sourceFeatureName, String targetLayerName,
                              int patchX, int patchY, double[] activations) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.frameId = frameId;
        this.sourceLayerName = sourceLayerName;
        this.sourceFeatureName = sourceFeatureName;
        this.targetLayerName = targetLayerName;
        this.patchX = patchX;
        this.patchY = patchY;
        setActivations(activations);
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getSourceLayerName() { return sourceLayerName; }
    public void setSourceLayerName(String sourceLayerName) { this.sourceLayerName = sourceLayerName; }
    public String getSourceFeatureName() { return sourceFeatureName; }
    public void setSourceFeatureName(String sourceFeatureName) { this.sourceFeatureName = sourceFeatureName; }
    public String getTargetLayerName() { return targetLayerName; }
    public void setTargetLayerName(String targetLayerName) { this.targetLayerName = targetLayerName; }
    public int getPatchX() { return patchX; }
    public void setPatchX(int patchX) { this.patchX = patchX; }
    public int getPatchY() { return patchY; }
    public void setPatchY(int patchY) { this.patchY = patchY; }
    public double[] getActivations() { return activations.clone(); }
    public void setActivations(double[] activations) {
        if (activations == null || activations.length != 9) {
            throw new IllegalArgumentException("FeaturePatchSignal requires exactly 9 feature activations");
        }
        this.activations = Arrays.copyOf(activations, 9);
    }
}
