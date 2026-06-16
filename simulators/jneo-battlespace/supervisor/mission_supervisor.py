from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any


class Backend(str, Enum):
    IN_MEMORY = "IN_MEMORY"
    JNEO_BATTLESPACE = "JNEO_BATTLESPACE"


class IntentType(str, Enum):
    ARM = "arm"
    TAKE_OFF = "take_off"
    HOLD = "hold"
    YAW = "yaw"
    FORWARD = "bounded_forward_movement"
    CLIMB = "bounded_climb_or_descent"
    SENSOR_WAYPOINT = "move_to_sensor_derived_waypoint"
    FRAME_TARGET = "reposition_for_camera_framing"
    RETURN_HOME = "return_to_home"
    LAND = "land"
    ABORT = "abort"
    SUBMIT_PHOTO = "submit_photo"


@dataclass(frozen=True)
class MissionIntent:
    intentId: str
    uavId: str
    intentType: IntentType
    simulationTime: float
    resetGeneration: int
    params: dict[str, Any] = field(default_factory=dict)


class JNeoBattlespaceMissionSupervisor:
    """Validates simulator-only flight command requests."""

    ALLOWED_INTENTS = {item.value for item in IntentType}

    def __init__(self, backend: Backend, simulator_only: bool, vehicle_ids: set[str]) -> None:
        self.backend = backend
        self.simulator_only = simulator_only
        self.vehicle_ids = vehicle_ids
        self.heartbeat_by_vehicle = {vehicle_id: 0.0 for vehicle_id in vehicle_ids}
        self.reset_generation = 0

    def heartbeat(self, uav_id: str, simulation_time: float) -> None:
        if uav_id in self.heartbeat_by_vehicle:
            self.heartbeat_by_vehicle[uav_id] = simulation_time

    def validate(self, intent: MissionIntent, execute: bool) -> dict:
        reasons: list[str] = []
        if intent.uavId not in self.vehicle_ids:
            reasons.append("VEHICLE_NOT_ALLOWLISTED")
        if intent.intentType.value not in self.ALLOWED_INTENTS:
            reasons.append("INTENT_NOT_SUPPORTED")
        if intent.resetGeneration != self.reset_generation:
            reasons.append("STALE_RESET_GENERATION")
        heartbeat_age = intent.simulationTime - self.heartbeat_by_vehicle.get(intent.uavId, -9999.0)
        if heartbeat_age > 2.0:
            reasons.append("STALE_HEARTBEAT")
        if not self._within_limits(intent):
            reasons.append("SAFETY_LIMIT_EXCEEDED")
        if execute and (self.backend != Backend.JNEO_BATTLESPACE or not self.simulator_only):
            reasons.append("COMMAND_EXECUTION_REQUIRES_JNEO_BATTLESPACE_SIMULATOR_ONLY")

        return {
            "intent": {
                "intentId": intent.intentId,
                "uavId": intent.uavId,
                "intentType": intent.intentType.value,
                "simulationTime": intent.simulationTime,
                "resetGeneration": intent.resetGeneration,
                "params": intent.params,
            },
            "execute": execute,
            "status": "ACCEPTED" if not reasons else "REJECTED",
            "reasons": reasons,
        }

    def reset(self, generation: int) -> None:
        self.reset_generation = generation
        for key in self.heartbeat_by_vehicle:
            self.heartbeat_by_vehicle[key] = 0.0

    @staticmethod
    def _within_limits(intent: MissionIntent) -> bool:
        altitude = float(intent.params.get("altitudeMeters", 0.0))
        speed = float(intent.params.get("speedMetersPerSecond", 0.0))
        acceleration = float(intent.params.get("accelerationMetersPerSecond2", 0.0))
        battery = float(intent.params.get("batteryFraction", 1.0))
        if altitude < 0.0 or altitude > 120.0:
            return False
        if speed < 0.0 or speed > 15.0:
            return False
        if acceleration < 0.0 or acceleration > 6.0:
            return False
        return battery >= 0.20
