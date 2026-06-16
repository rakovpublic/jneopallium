package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class PhotographyNeuron {
    public PhotographResultSignal photograph(PhotographRequestSignal request, ObservationTarget target, UavPose pose,
                                             UavSingleConfig config, long tick, int attempt) {
        PhotographResultSignal result = new PhotographResultSignal();
        result.setMissionId(request.getMissionId());
        result.setUavId(request.getUavId());
        result.setTick(tick);
        result.setTargetId(request.getTargetId());
        result.setPhotographId("photo-" + request.getMissionId() + "-" + request.getTargetId() + "-" + attempt);

        double range = pose.distance2d(target.x, target.y);
        double bearing = Math.toDegrees(Math.atan2(target.y - pose.y, target.x - pose.x));
        double quality = quality(range, target, pose, config);
        String reason = rejectionReason(request, target, pose, config, range, quality);
        boolean accepted = reason == null;
        result.setAccepted(accepted);
        result.setQualityScore(quality);
        result.setReason(accepted ? "PHOTO_ACCEPTED" : reason);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("photographId", result.getPhotographId());
        metadata.put("targetId", request.getTargetId());
        metadata.put("missionId", request.getMissionId());
        metadata.put("uavId", request.getUavId());
        metadata.put("simulationTick", tick);
        metadata.put("uavPose", Map.of("x", pose.x, "y", pose.y, "altitudeMeters", pose.altitudeMeters));
        metadata.put("targetPose", Map.of("x", target.x, "y", target.y));
        metadata.put("range", range);
        metadata.put("bearing", bearing);
        metadata.put("visibility", target.visibility);
        metadata.put("confidence", target.confidence);
        metadata.put("qualityScore", quality);
        metadata.put("contentHash", contentHash(request.getMissionId(), request.getTargetId(), tick, attempt));
        result.setAttributes(metadata);
        return result;
    }

    private static String rejectionReason(PhotographRequestSignal request, ObservationTarget target, UavPose pose,
                                          UavSingleConfig config, double range, double quality) {
        if (target == null || !target.active || !target.targetId.equals(request.getTargetId())) {
            return "TARGET_ID_INVALID";
        }
        if (range < config.prohibitedMinimumTargetDistanceMeters) {
            return "PROHIBITED_MINIMUM_DISTANCE";
        }
        double max = config.targetObservationDistanceMeters * 1.35;
        double min = config.targetObservationDistanceMeters * 0.70;
        if (range < min || range > max) {
            return "OUTSIDE_OBSERVATION_BAND";
        }
        if (!target.inFieldOfView) {
            return "TARGET_OUTSIDE_FIELD_OF_VIEW";
        }
        if (target.visibility < 0.55) {
            return "VISIBILITY_TOO_LOW";
        }
        if (target.motionBlurEstimate > 0.45) {
            return "MOTION_BLUR_TOO_HIGH";
        }
        if (pose.localizationConfidence < 0.55) {
            return "LOCALIZATION_CONFIDENCE_TOO_LOW";
        }
        if (pose.batteryFraction < config.batteryReserveFraction) {
            return "BATTERY_RESERVE_TOO_LOW";
        }
        if (quality < 0.62) {
            return "PHOTO_QUALITY_TOO_LOW";
        }
        return null;
    }

    private static double quality(double range, ObservationTarget target, UavPose pose, UavSingleConfig config) {
        double bandScore = 1.0 - Math.min(1.0, Math.abs(range - config.targetObservationDistanceMeters)
                / config.targetObservationDistanceMeters);
        double raw = 0.35 * bandScore
                + 0.25 * target.visibility
                + 0.20 * target.confidence
                + 0.10 * pose.localizationConfidence
                + 0.10 * (1.0 - target.motionBlurEstimate);
        return TargetPriorityProcessor.clamp(raw);
    }

    private static String contentHash(String missionId, String targetId, long tick, int attempt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((missionId + ":" + targetId + ":" + tick + ":" + attempt)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

