package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class PooledFeatureSignal extends UavSingleSignal {
    private String frameId;
    private String layerName;
    private String filterName;
    private double maxActivation;
    private double averageActivation;
    private double activePatchFraction;

    public PooledFeatureSignal() {
        setEventType("POOLED_CONVOLUTION_FEATURE");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getLayerName() { return layerName; }
    public void setLayerName(String layerName) { this.layerName = layerName; }
    public String getFilterName() { return filterName; }
    public void setFilterName(String filterName) { this.filterName = filterName; }
    public double getMaxActivation() { return maxActivation; }
    public void setMaxActivation(double maxActivation) { this.maxActivation = maxActivation; }
    public double getAverageActivation() { return averageActivation; }
    public void setAverageActivation(double averageActivation) { this.averageActivation = averageActivation; }
    public double getActivePatchFraction() { return activePatchFraction; }
    public void setActivePatchFraction(double activePatchFraction) { this.activePatchFraction = activePatchFraction; }
}
