package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class Geofence {
    public double minX = -500.0;
    public double maxX = 500.0;
    public double minY = -500.0;
    public double maxY = 500.0;

    public Geofence() {
    }

    public Geofence(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}

