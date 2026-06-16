package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UavSinglePhotographyTest {
    @Test
    void photographAcceptedOnlyInsideObservationAndQualityEnvelope() {
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-photo", TargetClassification.DAMAGED_INFRASTRUCTURE,
                100.0, 0.0);
        target.visibility = 0.9;
        target.confidence = 0.85;
        UavPose pose = new UavPose(65.0, 0.0, 35.0, 0.0, 0.9, 0.95);
        PhotographRequestSignal request = new PhotographRequestSignal(
                config.missionId, config.uavId, 7L, "photo-request", target.targetId);

        PhotographResultSignal result = new PhotographyNeuron().photograph(request, target, pose, config, 7L, 1);

        assertTrue(result.isAccepted());
        assertEquals("PHOTO_ACCEPTED", result.getReason());
        assertNotNull(result.getAttributes().get("contentHash"));
    }

    @Test
    void lowVisibilityProducesRetryableRejectionReason() {
        UavSingleConfig config = new UavSingleConfig();
        ObservationTarget target = new ObservationTarget("target-photo", TargetClassification.WILDFIRE_HOTSPOT,
                100.0, 0.0);
        target.visibility = 0.3;
        UavPose pose = new UavPose(65.0, 0.0, 35.0, 0.0, 0.9, 0.95);
        PhotographRequestSignal request = new PhotographRequestSignal(
                config.missionId, config.uavId, 7L, "photo-request", target.targetId);

        PhotographResultSignal result = new PhotographyNeuron().photograph(request, target, pose, config, 7L, 1);

        assertFalse(result.isAccepted());
        assertEquals("VISIBILITY_TOO_LOW", result.getReason());
    }
}
