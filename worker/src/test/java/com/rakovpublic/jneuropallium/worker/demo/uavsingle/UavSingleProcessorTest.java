package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UavSingleProcessorTest {
    @Test
    void priorityFormulaUsesAllFactorsAndStableTieBreak() {
        UavSingleConfig config = new UavSingleConfig();
        UavPose pose = new UavPose(0.0, 0.0, 35.0, 0.0, 0.9, 0.95);
        ObservationTarget target = target("target-a", 100.0, 0.0);

        TargetPriorityProcessor processor = new TargetPriorityProcessor();
        TargetPriority priority = processor.calculate(target, pose, config, 0.0);

        double expected = 0.0;
        for (Map.Entry<String, Double> factor : priority.factors.entrySet()) {
            expected += config.priorityWeights.get(factor.getKey()) * factor.getValue();
        }
        assertEquals(expected, priority.score, 1.0e-9);
        assertTrue(priority.factors.keySet().containsAll(config.priorityWeights.keySet()));

        TargetPriority tiedA = new TargetPriority("target-a", Map.of(), 0.75);
        TargetPriority tiedB = new TargetPriority("target-b", Map.of(), 0.75);
        assertEquals("target-a", processor.selectBest(List.of(tiedB, tiedA)).targetId);
    }

    @Test
    void confirmationProtocolRejectsMismatchedStaleAndDuplicateResponses() {
        ConfirmationProcessor processor = new ConfirmationProcessor();
        TargetConfirmationRequestSignal request = new TargetConfirmationRequestSignal(
                "mission-uav-single", "uav-1", 4L, "request-1", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, 8L);

        TargetConfirmationResponseSignal wrongRequest = new TargetConfirmationResponseSignal(
                "mission-uav-single", "uav-1", 5L, "request-wrong", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, ConfirmationDecision.APPROVE);
        assertEquals(ConfirmationStatus.REJECTED, processor.evaluate(request, wrongRequest, 5L).status);

        TargetConfirmationRequestSignal staleRequest = new TargetConfirmationRequestSignal(
                "mission-uav-single", "uav-1", 4L, "request-2", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, 8L);
        TargetConfirmationResponseSignal stale = new TargetConfirmationResponseSignal(
                "mission-uav-single", "uav-1", 9L, "request-2", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, ConfirmationDecision.APPROVE_TOO_LATE);
        assertEquals(ConfirmationStatus.REJECTED, processor.evaluate(staleRequest, stale, 9L).status);

        TargetConfirmationRequestSignal approvedRequest = new TargetConfirmationRequestSignal(
                "mission-uav-single", "uav-1", 4L, "request-3", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, 8L);
        TargetConfirmationResponseSignal approved = new TargetConfirmationResponseSignal(
                "mission-uav-single", "uav-1", 5L, "request-3", "target-a",
                UavActionType.APPROACH_OBSERVATION_POINT, ConfirmationDecision.APPROVE);
        assertEquals(ConfirmationStatus.APPROVED, processor.evaluate(approvedRequest, approved, 5L).status);
        assertEquals("DUPLICATE_CONFIRMATION", processor.evaluate(approvedRequest, approved, 6L).reason);
    }

    @Test
    void stateMachineRejectsImpossibleMissionJump() {
        UavMissionStateMachine machine = new UavMissionStateMachine();

        assertFalse(UavMissionStateMachine.canTransition(UavMissionState.INITIALIZING, UavMissionState.PHOTOGRAPHING));
        assertThrows(IllegalStateException.class, () -> machine.transition(UavMissionState.PHOTOGRAPHING));
    }

    private static ObservationTarget target(String targetId, double x, double y) {
        ObservationTarget target = new ObservationTarget(targetId, TargetClassification.DAMAGED_INFRASTRUCTURE, x, y);
        target.missionRelevance = 0.8;
        target.confidence = 0.7;
        target.urgency = 0.6;
        target.informationValue = 0.5;
        target.communicationValue = 0.4;
        target.safetyRisk = 0.1;
        return target;
    }
}
