package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class NoGoZone {
    public String zoneId;
    public double minX;
    public double maxX;
    public double minY;
    public double maxY;

    public NoGoZone() {
    }

    public NoGoZone(String zoneId, double minX, double maxX, double minY, double maxY) {
        this.zoneId = zoneId;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}

