package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class UavIntentSignal extends UavSingleSignal {
    private String intentId;
    private UavActionType actionType;
    private String targetId;
    private double destinationX;
    private double destinationY;
    private double altitudeMeters;
    private double speedMetersPerSecond;
    private long expiresAtTick;

    public UavIntentSignal() {
        setEventType("UAV_INTENT");
    }

    public String getIntentId() { return intentId; }
    public void setIntentId(String intentId) { this.intentId = intentId; }
    public UavActionType getActionType() { return actionType; }
    public void setActionType(UavActionType actionType) { this.actionType = actionType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public double getDestinationX() { return destinationX; }
    public void setDestinationX(double destinationX) { this.destinationX = destinationX; }
    public double getDestinationY() { return destinationY; }
    public void setDestinationY(double destinationY) { this.destinationY = destinationY; }
    public double getAltitudeMeters() { return altitudeMeters; }
    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }
    public double getSpeedMetersPerSecond() { return speedMetersPerSecond; }
    public void setSpeedMetersPerSecond(double speedMetersPerSecond) { this.speedMetersPerSecond = speedMetersPerSecond; }
    public long getExpiresAtTick() { return expiresAtTick; }
    public void setExpiresAtTick(long expiresAtTick) { this.expiresAtTick = expiresAtTick; }
}

