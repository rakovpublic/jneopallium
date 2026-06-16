from __future__ import annotations

from dataclasses import asdict, dataclass
from enum import Enum


class FieldClassification(str, Enum):
    AGENT_PUBLIC = "AGENT_PUBLIC"
    EVALUATOR_ONLY = "EVALUATOR_ONLY"
    TEST_ONLY = "TEST_ONLY"


@dataclass(frozen=True)
class SimulatorFieldMapping:
    source: str
    field: str
    classification: FieldClassification
    destination: str


class GroundTruthFirewall:
    AGENT_DESTINATIONS = {
        "jneopallium-input",
        "mavlink-advisory-input",
        "ros2-agent-namespace",
        "swarm-message",
        "target-report",
        "confirmation-request",
        "flight-intent",
    }

    DEFAULT_PERMITTED_TOPICS = {
        "/clock",
        "/uav_*/fpv/image",
        "/uav_*/fpv/camera_info",
        "/uav_*/imu",
        "/uav_*/range",
        "/uav_*/odometry",
        "/uav_*/battery",
        "/uav_*/radio_status",
    }

    DEFAULT_HIDDEN_TOPICS = {
        "/evaluator/targets",
        "/evaluator/scoring",
        "/world/*/pose/info",
        "/gazebo/entity_state",
        "/segmentation/ground_truth",
        "/target/lifecycle",
    }

    def validate(self, mappings: list[SimulatorFieldMapping], runtime_attempts: list[dict] | None = None) -> dict:
        rejected: list[dict] = []
        permitted_fields: list[dict] = []
        for mapping in mappings:
            row = asdict(mapping)
            row["classification"] = mapping.classification.value
            if (
                mapping.classification != FieldClassification.AGENT_PUBLIC
                and mapping.destination in self.AGENT_DESTINATIONS
            ):
                rejected.append(row | {"reason": "HIDDEN_FIELD_TO_AGENT_DESTINATION"})
            else:
                permitted_fields.append(row)

        leak_attempts = runtime_attempts or []
        for attempt in leak_attempts:
            if attempt.get("classification") in {
                FieldClassification.EVALUATOR_ONLY.value,
                FieldClassification.TEST_ONLY.value,
            }:
                rejected.append(attempt | {"reason": "RUNTIME_LEAK_ATTEMPT"})

        return {
            "permittedTopics": sorted(self.DEFAULT_PERMITTED_TOPICS),
            "hiddenTopics": sorted(self.DEFAULT_HIDDEN_TOPICS),
            "permittedSignalFields": permitted_fields,
            "rejectedMappings": rejected,
            "runtimeLeakAttempts": leak_attempts,
            "result": "PASS" if not rejected else "FAIL",
        }


def default_agent_mappings(vehicle_count: int) -> list[SimulatorFieldMapping]:
    mappings: list[SimulatorFieldMapping] = [
        SimulatorFieldMapping("/clock", "simulationTime", FieldClassification.AGENT_PUBLIC, "jneopallium-input"),
        SimulatorFieldMapping("/evaluator/targets", "targetEntityId", FieldClassification.EVALUATOR_ONLY, "evaluator"),
        SimulatorFieldMapping("/evaluator/scoring", "scoreLedger", FieldClassification.EVALUATOR_ONLY, "evaluator"),
        SimulatorFieldMapping("/test/oracle", "expectedRoute", FieldClassification.TEST_ONLY, "test-harness"),
    ]
    for number in range(1, vehicle_count + 1):
        namespace = f"/uav_{number}"
        mappings.extend(
            [
                SimulatorFieldMapping(
                    f"{namespace}/fpv/image",
                    "imageReference",
                    FieldClassification.AGENT_PUBLIC,
                    "ros2-agent-namespace",
                ),
                SimulatorFieldMapping(
                    f"{namespace}/imu",
                    "linearAcceleration",
                    FieldClassification.AGENT_PUBLIC,
                    "ros2-agent-namespace",
                ),
                SimulatorFieldMapping(
                    f"{namespace}/odometry",
                    "agentPermittedOdometry",
                    FieldClassification.AGENT_PUBLIC,
                    "ros2-agent-namespace",
                ),
            ]
        )
    return mappings

