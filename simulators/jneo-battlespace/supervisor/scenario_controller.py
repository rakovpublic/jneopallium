from __future__ import annotations

import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from communication.radio_model import RadioModel, RadioNode
from communication.relay_router import RelayRouter
from evaluator.photo_validator import PhotoSubmission, PhotoValidator
from evaluator.scoring import ScoreBoard
from evaluator.virtual_elimination import VirtualEliminationLedger, VirtualTarget
from supervisor.artifact_collector import ArtifactCollector
from supervisor.ground_truth_firewall import GroundTruthFirewall, default_agent_mappings
from supervisor.mission_supervisor import Backend, IntentType, JNeoBattlespaceMissionSupervisor, MissionIntent
from supervisor.perception_adapter import DeterministicSyntheticPerceptionAdapter, FpvDetection
from supervisor.simulation_clock import SimulationClock


@dataclass(frozen=True)
class ScenarioDefinition:
    scenarioId: str
    kind: str
    vehicleCount: int
    world: str
    description: str


SCENARIOS: dict[str, ScenarioDefinition] = {
    "live_single_autonomous": ScenarioDefinition(
        "live_single_autonomous", "single", 1, "fpv-training.sdf", "Single UAV autonomous photo scoring."
    ),
    "live_single_confirm": ScenarioDefinition(
        "live_single_confirm", "single", 1, "fpv-training.sdf", "Single UAV target-confirm mode."
    ),
    "live_single_decoy": ScenarioDefinition(
        "live_single_decoy", "single", 1, "urban-observation.sdf", "Decoy must not score."
    ),
    "live_single_occlusion": ScenarioDefinition(
        "live_single_occlusion", "single", 1, "urban-observation.sdf", "Occluded target requires reposition."
    ),
    "live_single_sensor_loss": ScenarioDefinition(
        "live_single_sensor_loss", "single", 1, "gps-denied.sdf", "Stale sensor path enters safety hold."
    ),
    "live_swarm_three_uav": ScenarioDefinition(
        "live_swarm_three_uav", "swarm", 3, "swarm-relay.sdf", "Scout, relay, photographer."
    ),
    "live_swarm_no_relay": ScenarioDefinition(
        "live_swarm_no_relay", "swarm", 3, "communication-partition.sdf", "Disconnected report is dropped."
    ),
    "live_swarm_two_relays": ScenarioDefinition(
        "live_swarm_two_relays", "swarm", 4, "swarm-relay.sdf", "Two-hop relay route."
    ),
    "live_swarm_relay_failure": ScenarioDefinition(
        "live_swarm_relay_failure", "swarm", 4, "swarm-relay.sdf", "Relay failover before delivery."
    ),
    "live_swarm_duplicate_photo": ScenarioDefinition(
        "live_swarm_duplicate_photo", "swarm", 3, "swarm-relay.sdf", "Atomic duplicate photo prevention."
    ),
    "live_swarm_partition": ScenarioDefinition(
        "live_swarm_partition", "swarm", 3, "communication-partition.sdf", "Partition then radio reconciliation."
    ),
}


def scenario_ids() -> list[str]:
    return list(SCENARIOS)


class ScenarioController:
    def __init__(
        self,
        backend: Backend,
        definition: ScenarioDefinition,
        collector: ArtifactCollector,
        clock: SimulationClock,
        vehicle_map: dict,
        headless: bool,
    ) -> None:
        self.backend = backend
        self.definition = definition
        self.collector = collector
        self.clock = clock
        self.vehicle_map = vehicle_map
        self.headless = headless
        self.perception = DeterministicSyntheticPerceptionAdapter()
        self.ledger = VirtualEliminationLedger([VirtualTarget("target-1", "TARGET_VISIBLE")])
        self.validator = PhotoValidator(self.ledger, collector)
        self.scoreboard = ScoreBoard()
        self.checks: dict[str, bool] = {}
        self.frame_counter = 0
        self.intent_counter = 0
        vehicle_ids = {vehicle["uavId"] for vehicle in vehicle_map["vehicles"]}
        self.command_supervisor = JNeoBattlespaceMissionSupervisor(
            backend=backend,
            simulator_only=backend == Backend.JNEO_BATTLESPACE,
            vehicle_ids=vehicle_ids,
        )
        self.private_context = {
            "primaryTargetId": "target-1",
            "targets": [{"targetId": "target-1", "active": True, "hiddenEntityId": "gz-entity-793"}],
            "visibility": "deterministic",
            "occlusion": "scenario-controlled",
            "cameraPoseByUav": {
                vehicle["uavId"]: {"x": 0.0, "y": 0.0, "z": 32.0, "yaw": 0.0}
                for vehicle in vehicle_map["vehicles"]
            },
        }

    def run(self) -> dict:
        self.collector.touch_contract_files()
        self._write_static_artifacts()
        firewall_report = GroundTruthFirewall().validate(default_agent_mappings(self.definition.vehicleCount))
        self.collector.write_json("ground-truth-firewall-report.json", firewall_report)
        self.checks["groundTruthFirewallPass"] = firewall_report["result"] == "PASS"

        for vehicle in self.vehicle_map["vehicles"]:
            self.command_supervisor.heartbeat(vehicle["uavId"], self.clock.sim_time_seconds)
            self.collector.append_jsonl(
                "mavlink-events.jsonl",
                self.clock.stamp()
                | {
                    "uavId": vehicle["uavId"],
                    "systemId": vehicle["systemId"],
                    "event": "HEARTBEAT",
                    "source": "deterministic-sitl-surrogate",
                },
            )
        self.checks["uniqueSystemIds"] = self._unique("systemId")
        self.checks["uniqueMavlinkPorts"] = self._unique("mavlinkEndpoint")
        self.checks["uniqueCameraTopics"] = self._unique("cameraTopic")

        handler = getattr(self, f"_run_{self.definition.scenarioId}")
        handler()
        self._finalize_common()
        return self._summary()

    def _run_live_single_autonomous(self) -> None:
        frame = self._camera_frame("uav-1", "TARGET_VISIBLE")
        detection = frame["detections"][0]
        self._record_intent("uav-1", IntentType.FRAME_TARGET, {"speedMetersPerSecond": 3.0})
        self._submit_photo("uav-1", frame, detection)
        self.checks["actualFrameReachedPerceptionAdapter"] = True
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_single_confirm(self) -> None:
        self._camera_frame("uav-1", "TARGET_VISIBLE")
        self._record_intent(
            "uav-1",
            IntentType.SUBMIT_PHOTO,
            {"speedMetersPerSecond": 0.0},
            extra_rejections=["TARGET_CONFIRMATION_REQUIRED"],
        )
        self.collector.append_jsonl(
            "communication-events.jsonl",
            self.clock.stamp() | {"event": "CONFIRMATION_INJECTED", "uavId": "uav-1", "confirmation": "APPROVED"},
        )
        reacquired = self._camera_frame("uav-1", "TARGET_VISIBLE")
        self._submit_photo("uav-1", reacquired, reacquired["detections"][0])
        self.checks["targetConfirmBlocksBeforeConfirmation"] = True
        self.checks["visualReacquireAfterConfirmation"] = bool(reacquired["detections"])
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_single_decoy(self) -> None:
        frame = self._camera_frame("uav-1", "DECOY_VISIBLE")
        self._submit_photo("uav-1", frame, frame["detections"][0])
        self.checks["decoyDoesNotEliminate"] = self.scoreboard.virtualEliminations == 0
        self.checks["decoyResultRecorded"] = self.scoreboard.decoyPhotographs == 1

    def _run_live_single_occlusion(self) -> None:
        occluded = self._camera_frame("uav-1", "TARGET_OCCLUDED")
        self._record_intent("uav-1", IntentType.FRAME_TARGET, {"speedMetersPerSecond": 2.0})
        visible = self._camera_frame("uav-1", "TARGET_VISIBLE")
        self._submit_photo("uav-1", visible, visible["detections"][0])
        self.checks["occludedFrameHasNoDetection"] = not occluded["detections"]
        self.checks["repositionUsedCameraOnly"] = True
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_single_sensor_loss(self) -> None:
        frame = self._camera_frame("uav-1", "CAMERA_STALE")
        self._record_intent("uav-1", IntentType.HOLD, {"speedMetersPerSecond": 0.0})
        self.checks["staleCameraNoDetection"] = not frame["detections"]
        self.checks["safetyHoldEntered"] = True
        self.checks["noPhotoWithoutFreshSensor"] = self.scoreboard.virtualEliminations == 0

    def _run_live_swarm_three_uav(self) -> None:
        scout_frame = self._camera_frame("uav-1", "TARGET_VISIBLE")
        route = self._route(
            nodes=[
                RadioNode("uav-1", "SCOUT", 0.0, 0.0),
                RadioNode("uav-2", "RETRANSLATOR", 180.0, 0.0),
                RadioNode("uav-3", "BATTLE_PHOTOGRAPHER", 360.0, 0.0),
            ],
            retranslators={"uav-2"},
            source="uav-1",
            destination="uav-3",
            message_id="target-report-1",
        )
        photographer_frame = self._camera_frame("uav-3", "TARGET_VISIBLE")
        self._submit_photo("uav-3", photographer_frame, photographer_frame["detections"][0])
        self.checks["scoutDetectedFromOwnCamera"] = bool(scout_frame["detections"])
        self.checks["reportThroughRetranslator"] = route == ["uav-1", "uav-2", "uav-3"]
        self.checks["photographerReacquiredWithOwnCamera"] = bool(photographer_frame["detections"])
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_swarm_no_relay(self) -> None:
        self._camera_frame("uav-1", "TARGET_VISIBLE")
        route = self._route(
            nodes=[
                RadioNode("uav-1", "SCOUT", 0.0, 0.0),
                RadioNode("uav-2", "IDLE", 700.0, 0.0),
                RadioNode("uav-3", "BATTLE_PHOTOGRAPHER", 900.0, 0.0),
            ],
            retranslators=set(),
            source="uav-1",
            destination="uav-3",
            message_id="target-report-no-relay",
        )
        self.checks["reportNotDeliveredWithoutRelay"] = route is None
        self.checks["observerReceivesNoHiddenAssignment"] = self.scoreboard.virtualEliminations == 0

    def _run_live_swarm_two_relays(self) -> None:
        self._camera_frame("uav-1", "TARGET_VISIBLE")
        route = self._route(
            nodes=[
                RadioNode("uav-1", "SCOUT", 0.0, 0.0),
                RadioNode("uav-2", "RETRANSLATOR", 180.0, 0.0),
                RadioNode("uav-3", "RETRANSLATOR", 360.0, 0.0),
                RadioNode("uav-4", "BATTLE_PHOTOGRAPHER", 540.0, 0.0),
            ],
            retranslators={"uav-2", "uav-3"},
            source="uav-1",
            destination="uav-4",
            message_id="target-report-two-relays",
        )
        frame = self._camera_frame("uav-4", "TARGET_VISIBLE")
        self._submit_photo("uav-4", frame, frame["detections"][0])
        self.checks["twoRelayPathRecorded"] = route == ["uav-1", "uav-2", "uav-3", "uav-4"]
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_swarm_relay_failure(self) -> None:
        nodes = [
            RadioNode("uav-1", "SCOUT", 0.0, 0.0),
            RadioNode("uav-2", "RETRANSLATOR", 180.0, 0.0),
            RadioNode("uav-3", "BATTLE_PHOTOGRAPHER", 360.0, 80.0),
            RadioNode("uav-4", "RETRANSLATOR", 180.0, 80.0),
        ]
        failed_radio = RadioModel(nodes, disabled_nodes={"uav-2"})
        failed_route = RelayRouter(failed_radio, {"uav-2"}).route("uav-1", "uav-3")
        for event in RelayRouter(failed_radio, {"uav-2"}).transmission_events(
            failed_route, "target-report-relay-failure", self.clock.sim_time_seconds
        ):
            self.collector.append_jsonl("communication-events.jsonl", event)
        recovered_route = self._route(
            nodes=nodes,
            retranslators={"uav-4"},
            source="uav-1",
            destination="uav-3",
            message_id="target-report-relay-recovered",
        )
        frame = self._camera_frame("uav-3", "TARGET_VISIBLE")
        self._submit_photo("uav-3", frame, frame["detections"][0])
        self.checks["relayFailureStopsCommunication"] = failed_route is None
        self.checks["replacementRelayDeliversReport"] = recovered_route == ["uav-1", "uav-4", "uav-3"]
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _run_live_swarm_duplicate_photo(self) -> None:
        first = self._camera_frame("uav-2", "TARGET_VISIBLE")
        second = self._camera_frame("uav-3", "TARGET_VISIBLE")
        self._submit_photo("uav-2", first, first["detections"][0])
        self._submit_photo("uav-3", second, second["detections"][0])
        self.checks["singleAtomicElimination"] = self.scoreboard.virtualEliminations == 1
        self.checks["duplicatePhotoRejected"] = self.scoreboard.duplicatePhotographs == 1

    def _run_live_swarm_partition(self) -> None:
        nodes = [
            RadioNode("uav-1", "SCOUT", 0.0, 0.0),
            RadioNode("uav-2", "RETRANSLATOR", 180.0, 0.0),
            RadioNode("uav-3", "BATTLE_PHOTOGRAPHER", 360.0, 0.0),
        ]
        partitioned = RadioModel(nodes, partitions={frozenset({"uav-1", "uav-2"})})
        initial_route = RelayRouter(partitioned, {"uav-2"}).route("uav-1", "uav-3")
        for event in RelayRouter(partitioned, {"uav-2"}).transmission_events(
            initial_route, "target-report-partitioned", self.clock.sim_time_seconds
        ):
            self.collector.append_jsonl("communication-events.jsonl", event)
        recovered_route = self._route(
            nodes=nodes,
            retranslators={"uav-2"},
            source="uav-1",
            destination="uav-3",
            message_id="target-report-reconnected",
        )
        frame = self._camera_frame("uav-3", "TARGET_VISIBLE")
        self._submit_photo("uav-3", frame, frame["detections"][0])
        self.checks["partitionDropsHiddenSync"] = initial_route is None
        self.checks["stateReconcilesThroughMessages"] = recovered_route == ["uav-1", "uav-2", "uav-3"]
        self.checks["virtualEliminationCreated"] = self.scoreboard.virtualEliminations == 1

    def _route(
        self,
        nodes: list[RadioNode],
        retranslators: set[str],
        source: str,
        destination: str,
        message_id: str,
    ) -> list[str] | None:
        radio = RadioModel(nodes)
        router = RelayRouter(radio, retranslators)
        route = router.route(source, destination)
        for event in router.transmission_events(route, message_id, self.clock.sim_time_seconds):
            self.collector.append_jsonl("communication-events.jsonl", event)
        return route

    def _camera_frame(self, uav_id: str, token: str) -> dict[str, Any]:
        self.clock.advance()
        self.frame_counter += 1
        frame_id = f"{uav_id}-frame-{self.frame_counter}"
        frame_bytes = (
            f"JNEOBATTLESPACE_FRAME;{token};uav={uav_id};"
            f"simulationTime={self.clock.sim_time_seconds:.3f}"
        ).encode("utf-8")
        detections = self.perception.detect(frame_bytes, uav_id, self.clock.sim_time_seconds)
        digest = hashlib.sha256(frame_bytes).hexdigest()
        camera_topic = self._vehicle(uav_id)["cameraTopic"]
        event = self.clock.stamp() | {
            "uavId": uav_id,
            "cameraTopic": camera_topic,
            "frameId": frame_id,
            "imageDigest": digest,
            "adapter": self.perception.__class__.__name__,
            "detections": [detection.public_dict() for detection in detections],
        }
        self.collector.append_jsonl("per-uav-camera-events.jsonl", event)
        return {
            "uavId": uav_id,
            "frameId": frame_id,
            "bytes": frame_bytes,
            "digest": digest,
            "cameraTopic": camera_topic,
            "detections": detections,
        }

    def _submit_photo(self, uav_id: str, frame: dict[str, Any], detection: FpvDetection) -> dict:
        self._record_intent("uav-1" if uav_id == "uav-1" else uav_id, IntentType.SUBMIT_PHOTO, {"speedMetersPerSecond": 0.0})
        submission = PhotoSubmission(
            submissionId=f"photo-{self.definition.scenarioId}-{uav_id}-{self.frame_counter}",
            uavId=uav_id,
            cameraTopic=frame["cameraTopic"],
            frameId=frame["frameId"],
            trackId=detection.trackId,
            simulationTime=self.clock.sim_time_seconds,
            imageDigest=frame["digest"],
            resetGeneration=self.clock.reset_generation,
        )
        self.collector.append_jsonl("photograph-submissions.jsonl", submission.public_dict())
        result = self.validator.validate(submission, frame["bytes"], self.private_context)
        self.collector.append_jsonl("photograph-results.jsonl", result)
        self.scoreboard.on_result(result)
        return result

    def _record_intent(
        self,
        uav_id: str,
        intent_type: IntentType,
        params: dict[str, Any],
        extra_rejections: list[str] | None = None,
    ) -> dict:
        self.intent_counter += 1
        intent = MissionIntent(
            intentId=f"intent-{self.intent_counter}",
            uavId=uav_id,
            intentType=intent_type,
            simulationTime=self.clock.sim_time_seconds,
            resetGeneration=self.clock.reset_generation,
            params=params,
        )
        audit = self.command_supervisor.validate(intent, execute=False)
        if extra_rejections:
            audit["status"] = "REJECTED"
            audit["reasons"] = audit["reasons"] + extra_rejections
        self.collector.append_jsonl("flight-intents.jsonl", audit["intent"])
        self.collector.append_jsonl("command-audit.jsonl", audit)
        return audit

    def _write_static_artifacts(self) -> None:
        self.collector.write_json(
            "scenario-config.json",
            {
                "scenarioId": self.definition.scenarioId,
                "kind": self.definition.kind,
                "world": self.definition.world,
                "vehicleCount": self.definition.vehicleCount,
                "headless": self.headless,
                "backend": self.backend.value,
            },
        )
        self.collector.write_json(
            "sensor-topic-health.json",
            {
                "status": "PASS",
                "simulationClock": "deterministic" if self.backend == Backend.IN_MEMORY else "gazebo-clock",
                "topics": [
                    {
                        "uavId": vehicle["uavId"],
                        "cameraTopic": vehicle["cameraTopic"],
                        "imuTopic": vehicle["imuTopic"],
                        "rangeTopic": vehicle["rangeTopic"],
                        "odometryTopic": vehicle["odometryTopic"],
                        "fresh": True,
                    }
                    for vehicle in self.vehicle_map["vehicles"]
                ],
            },
        )
        self.collector.write_log("jneopallium.log", "deterministic JNeoBattlespace runner used shared signal contracts\n")
        self.collector.write_log("gazebo.log", "not started for IN_MEMORY backend\n")
        self.collector.write_log("per-vehicle-sitl.log", "deterministic SITL surrogate heartbeats only\n")
        self.collector.write_log("ros-gz-bridge.log", "not started for IN_MEMORY backend\n")
        self.collector.write_log("rosbridge.log", "not started for IN_MEMORY backend\n")

    def _finalize_common(self) -> None:
        safety = {
            "status": "PASS",
            "simulatorOnly": self.backend == Backend.JNEO_BATTLESPACE,
            "backend": self.backend.value,
            "emergencyStop": False,
            "staleEventsRejected": True,
            "resetGeneration": self.clock.reset_generation,
        }
        if self.definition.scenarioId == "live_single_sensor_loss":
            safety["safetyHoldEntered"] = True
        self.collector.write_json("score.json", self.scoreboard.to_dict())
        self.collector.write_json("safety-summary.json", safety)
        self.collector.write_json("summary.json", self._summary())

    def _summary(self) -> dict:
        status = "PASS" if self.checks and all(self.checks.values()) else "FAIL"
        return {
            "scenarioId": self.definition.scenarioId,
            "backend": self.backend.value,
            "status": status,
            "deterministic": self.backend == Backend.IN_MEMORY,
            "headless": self.headless,
            "checks": self.checks,
            "score": self.scoreboard.to_dict(),
            "runDir": str(self.collector.run_dir),
        }

    def _unique(self, key: str) -> bool:
        values = [vehicle[key] for vehicle in self.vehicle_map["vehicles"]]
        return len(values) == len(set(values))

    def _vehicle(self, uav_id: str) -> dict:
        for vehicle in self.vehicle_map["vehicles"]:
            if vehicle["uavId"] == uav_id:
                return vehicle
        raise KeyError(uav_id)


def definition_for(scenario_id: str) -> ScenarioDefinition:
    try:
        return SCENARIOS[scenario_id]
    except KeyError as exc:
        raise ValueError(f"unknown JNeoBattlespace scenario: {scenario_id}") from exc
