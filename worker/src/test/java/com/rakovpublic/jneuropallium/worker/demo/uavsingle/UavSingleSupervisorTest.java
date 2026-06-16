package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UavSingleSupervisorTest {
    @Test
    void gatewayRejectsUnsafeIntentAndKeepsSimulatorCommandNonExecutable() {
        UavSingleConfig config = new UavSingleConfig();
        config.geofence = new Geofence(-20.0, 20.0, -20.0, 20.0);
        UavIntentSignal intent = intent(config, 50.0, 0.0);
        SimUavSupervisorContext context = new SimUavSupervisorContext(config,
                new UavPose(0.0, 0.0, 35.0, 0.0, 0.9, 0.95), 2L);

        SimUavCommandGateway.CommandDispatch dispatch =
                new SimUavCommandGateway(new SimUavMissionSupervisor()).dispatch(intent, context);

        assertFalse(dispatch.decision().accepted);
        assertTrue(dispatch.decision().reasons.contains("GEOFENCE_REJECTED"));
        assertFalse(dispatch.simulatorCommand().isExecute());
        assertTrue(dispatch.audit().containsKey("bridgeSafetyMode"));
    }

    @Test
    void supervisorRejectsNoGoBatteryAndHeartbeatFaults() {
        UavSingleConfig config = new UavSingleConfig();
        config.noGoZones = List.of(new NoGoZone("privacy-zone", 30.0, 60.0, -10.0, 10.0));
        UavIntentSignal intent = intent(config, 40.0, 0.0);
        SimUavSupervisorContext context = new SimUavSupervisorContext(config,
                new UavPose(0.0, 0.0, 35.0, 0.0, 0.15, 0.95), 2L);
        context.jneopalliumHeartbeatHealthy = false;

        SupervisorDecision decision = new SimUavMissionSupervisor().validate(intent, context);

        assertFalse(decision.accepted);
        assertTrue(decision.reasons.contains("JNEOPALLIUM_HEARTBEAT_STALE"));
        assertTrue(decision.reasons.contains("BATTERY_RESERVE_REJECTED"));
        assertTrue(decision.reasons.contains("NO_GO_ZONE_REJECTED:privacy-zone"));
    }

    private static UavIntentSignal intent(UavSingleConfig config, double x, double y) {
        UavIntentSignal signal = new UavIntentSignal();
        signal.setMissionId(config.missionId);
        signal.setUavId(config.uavId);
        signal.setTick(1L);
        signal.setExpiresAtTick(4L);
        signal.setIntentId("intent-test");
        signal.setActionType(UavActionType.APPROACH_OBSERVATION_POINT);
        signal.setTargetId("target-a");
        signal.setDestinationX(x);
        signal.setDestinationY(y);
        signal.setAltitudeMeters(35.0);
        signal.setSpeedMetersPerSecond(4.0);
        return signal;
    }
}
