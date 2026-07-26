package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class ConvolutionFeatureSignal extends UavSingleSignal {
    private String frameId;
    private String layerName;
    private String filterName;
    private int patchX;
    private int patchY;
    private double preActivation;
    private double activation;

    public ConvolutionFeatureSignal() {
        setEventType("CONVOLUTION_FEATURE");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getLayerName() { return layerName; }
    public void setLayerName(String layerName) { this.layerName = layerName; }
    public String getFilterName() { return filterName; }
    public void setFilterName(String filterName) { this.filterName = filterName; }
    public int getPatchX() { return patchX; }
    public void setPatchX(int patchX) { this.patchX = patchX; }
    public int getPatchY() { return patchY; }
    public void setPatchY(int patchY) { this.patchY = patchY; }
    public double getPreActivation() { return preActivation; }
    public void setPreActivation(double preActivation) { this.preActivation = preActivation; }
    public double getActivation() { return activation; }
    public void setActivation(double activation) { this.activation = activation; }
}
