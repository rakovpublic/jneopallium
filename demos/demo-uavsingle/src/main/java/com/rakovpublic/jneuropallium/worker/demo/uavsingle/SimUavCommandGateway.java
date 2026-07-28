package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;

import java.util.LinkedHashMap;
import java.util.Map;

public class SimUavCommandGateway {
    private final SimUavMissionSupervisor supervisor;

    public SimUavCommandGateway(SimUavMissionSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    public CommandDispatch dispatch(UavIntentSignal intent, SimUavSupervisorContext context) {
        SupervisorDecision decision = supervisor.validate(intent, context);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("tick", context.tick);
        audit.put("missionId", intent.getMissionId());
        audit.put("uavId", intent.getUavId());
        audit.put("intentId", intent.getIntentId());
        audit.put("action", intent.getActionType());
        audit.put("simulatorOnly", context.config.simulatorOnly);
        audit.put("bridgeSafetyMode", "SIMULATOR_ONLY");
        audit.put("status", decision.accepted ? "ACCEPTED" : "REJECTED");
        audit.put("reasons", decision.reasons);
        MotorCommandSignal simulatorCommand = new MotorCommandSignal(0, new double[]{
                intent.getDestinationX(), intent.getDestinationY(), intent.getAltitudeMeters(), intent.getSpeedMetersPerSecond()
        });
        simulatorCommand.setActionPlanId(intent.getIntentId());
        simulatorCommand.setExecute(false);
        return new CommandDispatch(decision, audit, simulatorCommand);
    }

    public record CommandDispatch(SupervisorDecision decision, Map<String, Object> audit,
                                  MotorCommandSignal simulatorCommand) {
    }
}

