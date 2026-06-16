package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.Arrays;

public class PixelPatchSignal extends UavSingleSignal {
    private String frameId;
    private String layerName;
    private int patchX;
    private int patchY;
    private double[] pixels = new double[9];

    public PixelPatchSignal() {
        setEventType("PIXEL_PATCH_3X3");
    }

    public PixelPatchSignal(String missionId, String uavId, long tick, String frameId,
                            String layerName, int patchX, int patchY, double[] pixels) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.frameId = frameId;
        this.layerName = layerName;
        this.patchX = patchX;
        this.patchY = patchY;
        setPixels(pixels);
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getLayerName() { return layerName; }
    public void setLayerName(String layerName) { this.layerName = layerName; }
    public int getPatchX() { return patchX; }
    public void setPatchX(int patchX) { this.patchX = patchX; }
    public int getPatchY() { return patchY; }
    public void setPatchY(int patchY) { this.patchY = patchY; }
    public double[] getPixels() { return pixels.clone(); }
    public void setPixels(double[] pixels) {
        if (pixels == null || pixels.length != 9) {
            throw new IllegalArgumentException("PixelPatchSignal requires exactly 9 normalized pixel values");
        }
        this.pixels = Arrays.copyOf(pixels, 9);
    }
}
