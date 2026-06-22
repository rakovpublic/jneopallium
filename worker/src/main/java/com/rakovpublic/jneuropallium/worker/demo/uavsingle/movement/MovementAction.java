package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One discrete motor option the movement policy can choose: a horizontal heading, an altitude
 * delta and a speed multiplier (plus a {@code behavior} hook for the special
 * {@code return_center} action whose direction is resolved at decision time from the area bounds).
 */
public final class MovementAction {
    public static final String BEHAVIOR_FIXED = "fixed";
    public static final String BEHAVIOR_RETURN_CENTER = "return_center";

    private final String actionId;
    private final String label;
    private final double directionX;
    private final double directionY;
    private final double altitudeDeltaMeters;
    private final double speedMultiplier;
    private final String behavior;

    public MovementAction(String actionId, String label, double directionX, double directionY,
                          double altitudeDeltaMeters, double speedMultiplier) {
        this(actionId, label, directionX, directionY, altitudeDeltaMeters, speedMultiplier, BEHAVIOR_FIXED);
    }

    public MovementAction(String actionId, String label, double directionX, double directionY,
                          double altitudeDeltaMeters, double speedMultiplier, String behavior) {
        this.actionId = actionId;
        this.label = label;
        this.directionX = directionX;
        this.directionY = directionY;
        this.altitudeDeltaMeters = altitudeDeltaMeters;
        this.speedMultiplier = speedMultiplier;
        this.behavior = behavior == null ? BEHAVIOR_FIXED : behavior;
    }

    public String getActionId() { return actionId; }
    public String getLabel() { return label; }
    public double getDirectionX() { return directionX; }
    public double getDirectionY() { return directionY; }
    public double getAltitudeDeltaMeters() { return altitudeDeltaMeters; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public String getBehavior() { return behavior; }

    public boolean isReturnCenter() { return BEHAVIOR_RETURN_CENTER.equals(behavior); }

    public Map<String, Object> asModelMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("actionId", actionId);
        map.put("label", label);
        map.put("directionX", directionX);
        map.put("directionY", directionY);
        map.put("altitudeDeltaMeters", altitudeDeltaMeters);
        map.put("speedMultiplier", speedMultiplier);
        map.put("behavior", behavior);
        return map;
    }
}
