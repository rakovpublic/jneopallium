package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class UavPose {
    public double x;
    public double y;
    public double altitudeMeters;
    public double yawDegrees;
    public double batteryFraction = 1.0;
    public double localizationConfidence = 1.0;

    public UavPose() {
    }

    public UavPose(double x, double y, double altitudeMeters, double yawDegrees,
                   double batteryFraction, double localizationConfidence) {
        this.x = x;
        this.y = y;
        this.altitudeMeters = altitudeMeters;
        this.yawDegrees = yawDegrees;
        this.batteryFraction = batteryFraction;
        this.localizationConfidence = localizationConfidence;
    }

    public double distance2d(double targetX, double targetY) {
        double dx = x - targetX;
        double dy = y - targetY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public UavPose copy() {
        return new UavPose(x, y, altitudeMeters, yawDegrees, batteryFraction, localizationConfidence);
    }
}

