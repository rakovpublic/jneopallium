package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class SearchArea {
    public String areaId = "default-search-area";
    public double minX = 0.0;
    public double maxX = 160.0;
    public double minY = -60.0;
    public double maxY = 60.0;
    public double altitudeMeters = 35.0;
    public double spacingMeters = 80.0;
    public double detectionRadiusMeters = 105.0;

    public SearchArea() {
    }

    public SearchArea(String areaId, double minX, double maxX, double minY, double maxY,
                      double altitudeMeters, double spacingMeters, double detectionRadiusMeters) {
        this.areaId = areaId;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.altitudeMeters = altitudeMeters;
        this.spacingMeters = spacingMeters;
        this.detectionRadiusMeters = detectionRadiusMeters;
    }

    public void validate(UavSingleConfig config) {
        if (areaId == null || areaId.isBlank()) {
            throw new IllegalArgumentException("searchArea.areaId is required");
        }
        if (maxX < minX || maxY < minY) {
            throw new IllegalArgumentException("searchArea bounds are invalid");
        }
        if (spacingMeters <= 0.0) {
            throw new IllegalArgumentException("searchArea.spacingMeters must be positive");
        }
        if (detectionRadiusMeters <= 0.0) {
            throw new IllegalArgumentException("searchArea.detectionRadiusMeters must be positive");
        }
        if (altitudeMeters < 0.0 || altitudeMeters > config.maximumAltitudeMeters) {
            throw new IllegalArgumentException("searchArea.altitudeMeters must be inside configured altitude bounds");
        }
    }

    public boolean contains(double x, double y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}
