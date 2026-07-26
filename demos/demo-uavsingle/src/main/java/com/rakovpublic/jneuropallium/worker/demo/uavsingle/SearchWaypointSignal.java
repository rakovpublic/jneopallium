package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

public class SearchWaypointSignal extends UavSingleSignal {
    private String areaId;
    private int waypointIndex;
    private double x;
    private double y;
    private double altitudeMeters;

    public SearchWaypointSignal() {
        setEventType("SEARCH_WAYPOINT");
    }

    public SearchWaypointSignal(String missionId, String uavId, long tick, String areaId,
                                int waypointIndex, UavPose waypoint) {
        this();
        setMissionId(missionId);
        setUavId(uavId);
        setTick(tick);
        this.areaId = areaId;
        this.waypointIndex = waypointIndex;
        this.x = waypoint.x;
        this.y = waypoint.y;
        this.altitudeMeters = waypoint.altitudeMeters;
    }

    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
    public int getWaypointIndex() { return waypointIndex; }
    public void setWaypointIndex(int waypointIndex) { this.waypointIndex = waypointIndex; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public double getAltitudeMeters() { return altitudeMeters; }
    public void setAltitudeMeters(double altitudeMeters) { this.altitudeMeters = altitudeMeters; }
}
