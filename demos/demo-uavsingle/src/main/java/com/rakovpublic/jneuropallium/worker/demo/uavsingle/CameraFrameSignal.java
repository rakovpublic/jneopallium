package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class CameraFrameSignal extends UavSingleSignal {
    private String frameId;
    private String trackId;
    private double frameCenterX;
    private double frameCenterY;
    private int width;
    private int height;
    private int[][] pixels;

    public CameraFrameSignal() {
        setEventType("CAMERA_FRAME");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public double getFrameCenterX() { return frameCenterX; }
    public void setFrameCenterX(double frameCenterX) { this.frameCenterX = frameCenterX; }
    public double getFrameCenterY() { return frameCenterY; }
    public void setFrameCenterY(double frameCenterY) { this.frameCenterY = frameCenterY; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public int[][] getPixels() { return pixels; }
    public void setPixels(int[][] pixels) {
        this.pixels = copyPixels(pixels);
        this.height = pixels == null ? 0 : pixels.length;
        this.width = this.height == 0 ? 0 : pixels[0].length;
    }

    static int[][] copyPixels(int[][] pixels) {
        if (pixels == null) {
            return new int[0][0];
        }
        int[][] copy = new int[pixels.length][];
        for (int row = 0; row < pixels.length; row++) {
            copy[row] = pixels[row] == null ? new int[0] : pixels[row].clone();
        }
        return copy;
    }
}
