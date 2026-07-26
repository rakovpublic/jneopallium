package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class SearchAreaSignal extends UavSingleSignal {
    private String areaId;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double spacingMeters;
    private double detectionRadiusMeters;

    public SearchAreaSignal() {
        setEventType("SEARCH_AREA");
    }

    public SearchAreaSignal(String missionId, String uavId, long tick, SearchArea area) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.areaId = area.areaId;
        this.minX = area.minX;
        this.maxX = area.maxX;
        this.minY = area.minY;
        this.maxY = area.maxY;
        this.spacingMeters = area.spacingMeters;
        this.detectionRadiusMeters = area.detectionRadiusMeters;
    }

    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }
    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }
    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }
    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }
    public double getSpacingMeters() { return spacingMeters; }
    public void setSpacingMeters(double spacingMeters) { this.spacingMeters = spacingMeters; }
    public double getDetectionRadiusMeters() { return detectionRadiusMeters; }
    public void setDetectionRadiusMeters(double detectionRadiusMeters) { this.detectionRadiusMeters = detectionRadiusMeters; }
}
