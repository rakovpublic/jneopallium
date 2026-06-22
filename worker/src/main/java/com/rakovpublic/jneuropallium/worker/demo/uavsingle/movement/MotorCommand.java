package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Concrete motor command the policy emits for the chosen action. It is the payload of a
 * {@link com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal} (effector 1 = flight
 * controller): a horizontal velocity, a target altitude, a hold duration and a yaw. The CARLA-Air
 * bridge maps this directly onto an AirSim {@code moveByVelocityZAsync} call.
 */
public final class MotorCommand {
    private final String actionPlanId;
    private final double velocityX;
    private final double velocityY;
    private final double targetAltitudeMeters;
    private final double durationSeconds;
    private final double yawDegrees;

    public MotorCommand(String actionPlanId, double velocityX, double velocityY,
                        double targetAltitudeMeters, double durationSeconds, double yawDegrees) {
        this.actionPlanId = actionPlanId;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.targetAltitudeMeters = targetAltitudeMeters;
        this.durationSeconds = durationSeconds;
        this.yawDegrees = yawDegrees;
    }

    public String getActionPlanId() { return actionPlanId; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getTargetAltitudeMeters() { return targetAltitudeMeters; }
    public double getDurationSeconds() { return durationSeconds; }
    public double getYawDegrees() { return yawDegrees; }

    /** Materialize the framework signal (effector 1, params [vx, vy, targetZ, duration, yaw]). */
    public MotorCommandSignal toSignal() {
        MotorCommandSignal signal = new MotorCommandSignal(1,
                new double[] {velocityX, velocityY, targetAltitudeMeters, durationSeconds, yawDegrees});
        signal.setActionPlanId(actionPlanId);
        return signal;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("signalClass", MotorCommandSignal.class.getName());
        map.put("effectorId", 1);
        map.put("execute", true);
        map.put("actionPlanId", actionPlanId);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("velocityX", round(velocityX));
        params.put("velocityY", round(velocityY));
        params.put("targetAltitudeMeters", round(targetAltitudeMeters));
        params.put("durationSeconds", round(durationSeconds));
        params.put("yawDegrees", round(yawDegrees));
        map.put("params", params);
        return map;
    }

    private static double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
