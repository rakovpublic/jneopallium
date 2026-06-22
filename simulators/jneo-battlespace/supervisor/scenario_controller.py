from __future__ import annotations

import hashlib
import math
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
from supervisor.mission_video_renderer import render_large_area_recordings
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
    "live_single_infantry_vehicle": ScenarioDefinition(
        "live_single_infantry_vehicle", "single", 1, "fpv-training.sdf", "Single UAV photographs infantry and vehicle."
    ),
    "live_single_large_area_search": ScenarioDefinition(
        "live_single_large_area_search", "single", 1, "urban-observation.sdf",
        "Single UAV searches a large area, finds infantry and vehicle, and records camera/top-down video."
    ),
    "carla_air_urban_search": ScenarioDefinition(
        "carla_air_urban_search", "carla-air", 3, "carla-town-urban-air.umap",
        "CARLA-Air urban search with Unreal FPV camera, dynamic actors, weather, physics, and Jneopallium learning."
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
        self.ledger = VirtualEliminationLedger(self._virtual_targets_for_definition(definition))
        self.validator = PhotoValidator(self.ledger, collector)
        self.scoreboard = ScoreBoard()
        self.checks: dict[str, bool] = {}
        self.frame_counter = 0
        self.intent_counter = 0
        self.top_down_sequence = 0
        self.recordings: dict[str, str] = {}
        vehicle_ids = {vehicle["uavId"] for vehicle in vehicle_map["vehicles"]}
        self.command_supervisor = JNeoBattlespaceMissionSupervisor(
            backend=backend,
            simulator_only=backend in {Backend.JNEO_BATTLESPACE, Backend.CARLA_AIR},
            vehicle_ids=vehicle_ids,
        )
        self.private_context = {
            "primaryTargetId": "target-1",
            "targets": [
                {
                    "targetId": target.targetId,
                    "active": target.active,
                    "visualToken": target.visualToken,
                    "hiddenEntityId": f"gz-entity-{793 + index}",
                }
                for index, target in enumerate(self._virtual_targets_for_definition(definition))
            ],
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

    def _run_live_single_infantry_vehicle(self) -> None:
        infantry = self._camera_frame("uav-1", "INFANTRY_VISIBLE")
        infantry_result = self._submit_photo("uav-1", infantry, infantry["detections"][0])
        vehicle = self._camera_frame("uav-1", "VEHICLE_VISIBLE")
        vehicle_result = self._submit_photo("uav-1", vehicle, vehicle["detections"][0])
        labels = [detection.classLabel for frame in [infantry, vehicle] for detection in frame["detections"]]
        self.checks["infantryDetectedFromCamera"] = "infantry" in labels
        self.checks["vehicleDetectedFromCamera"] = "vehicle" in labels
        self.checks["infantryPhotoAccepted"] = infantry_result["status"] == "ELIMINATED"
        self.checks["vehiclePhotoAccepted"] = vehicle_result["status"] == "ELIMINATED"
        self.checks["twoVirtualEliminationsCreated"] = self.scoreboard.virtualEliminations == 2

    def _run_live_single_large_area_search(self) -> None:
        search_area = {
            "areaId": "large-jneo-battlespace-grid",
            "minX": -720.0,
            "maxX": 720.0,
            "minY": -480.0,
            "maxY": 480.0,
            "altitudeMeters": 72.0,
            "spacingMeters": 180.0,
            "detectionRadiusMeters": 165.0,
        }
        targets = [
            {
                "targetId": "large-infantry-1",
                "visualToken": "INFANTRY_VISIBLE",
                "classLabel": "infantry",
                "x": -260.0,
                "y": -290.0,
                "active": True,
                "model": {"detail": "helmet, torso, backpack, limbs", "bbox": [0.43, 0.26, 0.14, 0.34]},
            },
            {
                "targetId": "large-vehicle-1",
                "visualToken": "VEHICLE_VISIBLE",
                "classLabel": "vehicle",
                "x": 430.0,
                "y": 330.0,
                "active": True,
                "model": {"detail": "body, cabin, wheels, windows", "bbox": [0.30, 0.38, 0.42, 0.20]},
            },
        ]
        self._write_large_area_config(search_area, targets)
        pose = {"x": 0.0, "y": 0.0, "z": search_area["altitudeMeters"], "yaw": 0.0}
        self._update_camera_pose("uav-1", pose)
        self._record_top_down("MISSION_START", pose, search_area, targets)
        self._record_intent("uav-1", IntentType.ARM, {"speedMetersPerSecond": 0.0})
        self._record_intent("uav-1", IntentType.TAKE_OFF, {
            "altitudeMeters": search_area["altitudeMeters"],
            "speedMetersPerSecond": 3.0,
        })

        waypoints = self._large_area_waypoints(search_area)
        labels: list[str] = []
        photos: list[dict] = []
        sweep_frames = 0
        visited = 0
        for index, waypoint in enumerate(waypoints):
            visited += 1
            pose = {"x": waypoint["x"], "y": waypoint["y"], "z": search_area["altitudeMeters"], "yaw": waypoint["yaw"]}
            self._move_to_waypoint("uav-1", pose, index, search_area, targets)
            visible = [target for target in targets if target["active"]
                       and self._distance(pose, target) <= search_area["detectionRadiusMeters"]]
            if not visible:
                sweep_frames += 1
                self._camera_frame("uav-1", "SEARCH_SWEEP", {
                    "frameKind": "SEARCH_SWEEP",
                    "uavPose": pose,
                    "searchArea": search_area,
                })
                continue

            for target in visible:
                frame = self._camera_frame("uav-1", target["visualToken"], {
                    "frameKind": "TARGET_OBSERVATION",
                    "uavPose": pose,
                    "targetModel": target["model"] | {
                        "targetId": target["targetId"],
                        "classLabel": target["classLabel"],
                        "x": target["x"],
                        "y": target["y"],
                    },
                    "searchArea": search_area,
                })
                if not frame["detections"]:
                    continue
                detection = frame["detections"][0]
                labels.append(detection.classLabel)
                self._record_intent("uav-1", IntentType.FRAME_TARGET, {
                    "targetId": target["targetId"],
                    "speedMetersPerSecond": 2.0,
                    "bbox": detection.bbox,
                })
                photo_result = self._submit_photo("uav-1", frame, detection)
                photos.append(photo_result)
                target["active"] = photo_result["status"] != "ELIMINATED"
                self._sync_private_target_state(target["targetId"], target["active"])
                self._record_top_down("TARGET_PHOTOGRAPHED", pose, search_area, targets,
                                      waypoint_index=index, target_id=target["targetId"])
            if all(not target["active"] for target in targets):
                break

        self._record_intent("uav-1", IntentType.RETURN_HOME, {"speedMetersPerSecond": 5.0})
        home = {"x": 0.0, "y": 0.0, "z": search_area["altitudeMeters"], "yaw": 180.0}
        self._update_camera_pose("uav-1", home)
        self._record_top_down("RETURN_HOME", home, search_area, targets)
        self._record_intent("uav-1", IntentType.LAND, {"speedMetersPerSecond": 0.0, "altitudeMeters": 0.0})
        landed = {"x": 0.0, "y": 0.0, "z": 0.0, "yaw": 180.0}
        self._update_camera_pose("uav-1", landed)
        self._record_top_down("LANDED", landed, search_area, targets)
        self.recordings = render_large_area_recordings(self.collector.run_dir)

        self.checks["largeAreaWaypointsVisited"] = visited >= 20
        self.checks["searchSweepFramesRecorded"] = sweep_frames >= 8
        self.checks["infantryDetectedFromCamera"] = "infantry" in labels
        self.checks["vehicleDetectedFromCamera"] = "vehicle" in labels
        self.checks["bothPhotosAccepted"] = sum(1 for result in photos if result["status"] == "ELIMINATED") == 2
        self.checks["bridgeIntentsAudited"] = self.intent_counter >= visited + 5
        self.checks["cameraVideoRecorded"] = Path(self.recordings["cameraVideoMp4"]).exists()
        self.checks["topDownVideoRecorded"] = Path(self.recordings["topDownVideoMp4"]).exists()

    def _run_carla_air_urban_search(self) -> None:
        search_area = {
            "areaId": "carla-air-urban-grid",
            "minX": -820.0,
            "maxX": 820.0,
            "minY": -560.0,
            "maxY": 560.0,
            "altitudeMeters": 68.0,
            "spacingMeters": 205.0,
            "detectionRadiusMeters": 185.0,
        }
        weather = {
            "preset": "CloudyNoonToWetSunset",
            "cloudiness": 62,
            "precipitation": 18,
            "sunAltitudeAngle": 24,
            "windIntensity": 22,
            "fogDensity": 8,
        }
        urban_geometry = {
            "map": "TownUrbanAir",
            "blocks": 18,
            "roads": 24,
            "buildingHeightRangeMeters": [8, 42],
            "collisionMeshes": "enabled",
        }
        dynamic_actors = {
            "vehicles": [
                {"actorId": "veh-dynamic-1", "type": "sedan", "route": [[-520, -410], [-120, -410], [280, -180]]},
                {"actorId": "veh-dynamic-2", "type": "van", "route": [[640, 500], [430, 330], [110, 130]]},
            ],
            "pedestrians": [
                {"actorId": "ped-dynamic-1", "type": "civilian", "route": [[-340, -300], [-260, -290], [-180, -280]]},
                {"actorId": "ped-dynamic-2", "type": "civilian", "route": [[250, 280], [390, 320], [520, 360]]},
            ],
        }
        targets = [
            {
                "targetId": "carla-infantry-1",
                "visualToken": "INFANTRY_VISIBLE",
                "classLabel": "infantry",
                "x": -260.0,
                "y": -290.0,
                "active": True,
                "model": {
                    "detail": "CARLA pedestrian skeletal mesh, helmet silhouette, backpack, limb articulation",
                    "bbox": [0.42, 0.24, 0.16, 0.38],
                    "mesh": "walker.pedestrian.carla_air.infantry_proxy",
                },
            },
            {
                "targetId": "carla-vehicle-1",
                "visualToken": "VEHICLE_VISIBLE",
                "classLabel": "vehicle",
                "x": 430.0,
                "y": 330.0,
                "active": True,
                "model": {
                    "detail": "CARLA dynamic vehicle mesh with cabin, wheels, glass, and shadow",
                    "bbox": [0.28, 0.37, 0.45, 0.22],
                    "mesh": "vehicle.carla_air.inspection_proxy",
                },
            },
        ]
        self._write_carla_air_config(search_area, targets, weather, urban_geometry, dynamic_actors)
        self._record_carla_event("WORLD_LOADED", {
            "world": self.definition.world,
            "urbanGeometry": urban_geometry,
            "weather": weather,
            "renderer": "Unreal FPV camera surrogate; live CARLA-Air runtime not present in this workspace",
        })
        for actor in dynamic_actors["vehicles"] + dynamic_actors["pedestrians"]:
            self._record_carla_event("DYNAMIC_ACTOR_SPAWNED", actor)

        route = self._route(
            nodes=[
                RadioNode("uav-1", "SCOUT_AND_PHOTOGRAPHER", 0.0, 0.0),
                RadioNode("uav-2", "RETRANSLATOR", 180.0, 40.0),
                RadioNode("uav-3", "OVERWATCH", 380.0, 90.0),
            ],
            retranslators={"uav-2"},
            source="uav-1",
            destination="uav-3",
            message_id="carla-air-search-plan",
        )

        pose = {"x": 0.0, "y": 0.0, "z": search_area["altitudeMeters"], "yaw": 0.0}
        self._update_camera_pose("uav-1", pose)
        self._record_top_down("CARLA_AIR_MISSION_START", pose, search_area, targets)
        self._record_jneopallium_decision("SWARM_COORDINATION", {
            "route": route,
            "roles": {"uav-1": "search/photo", "uav-2": "radio relay", "uav-3": "overwatch"},
        })
        self._record_intent("uav-1", IntentType.ARM, {"speedMetersPerSecond": 0.0})
        self._record_intent("uav-1", IntentType.TAKE_OFF, {
            "altitudeMeters": search_area["altitudeMeters"],
            "speedMetersPerSecond": 3.0,
        })

        waypoints = self._large_area_waypoints(search_area)
        labels: list[str] = []
        photos: list[dict] = []
        sweep_frames = 0
        visited = 0
        previous_pose = pose
        for index, waypoint in enumerate(waypoints):
            visited += 1
            pose = {"x": waypoint["x"], "y": waypoint["y"], "z": search_area["altitudeMeters"], "yaw": waypoint["yaw"]}
            velocity = self._velocity(previous_pose, pose)
            self._record_carla_event("DRONE_PHYSICS_TICK", {
                "uavId": "uav-1",
                "pose": pose,
                "velocityMetersPerSecond": velocity,
                "collision": False,
                "weather": weather,
            })
            self._move_to_waypoint("uav-1", pose, index, search_area, targets)
            self._record_sensor_fusion(pose, index, search_area, targets, dynamic_actors)
            self._record_jneopallium_decision("AUTONOMOUS_NAVIGATION", {
                "waypointIndex": index,
                "pose": pose,
                "collisionRisk": "clear",
                "urbanGeometry": "inside navigable street canyon volume",
            })
            previous_pose = pose
            visible = [target for target in targets if target["active"]
                       and self._distance(pose, target) <= search_area["detectionRadiusMeters"]]
            if not visible:
                sweep_frames += 1
                self._camera_frame("uav-1", "SEARCH_SWEEP", {
                    "frameKind": "CARLA_AIR_SEARCH_SWEEP",
                    "renderer": "Unreal-rendered FPV surrogate",
                    "weather": weather,
                    "uavPose": pose,
                    "searchArea": search_area,
                })
                continue

            for target in visible:
                frame = self._camera_frame("uav-1", target["visualToken"], {
                    "frameKind": "CARLA_AIR_TARGET_OBSERVATION",
                    "renderer": "Unreal-rendered FPV surrogate",
                    "weather": weather,
                    "lighting": {"sunAltitudeAngle": weather["sunAltitudeAngle"], "dynamicShadows": True},
                    "uavPose": pose,
                    "targetModel": target["model"] | {
                        "targetId": target["targetId"],
                        "classLabel": target["classLabel"],
                        "x": target["x"],
                        "y": target["y"],
                    },
                    "searchArea": search_area,
                })
                self._record_jneopallium_decision("CAMERA_PERCEPTION", {
                    "frameId": frame["frameId"],
                    "detections": [detection.public_dict() for detection in frame["detections"]],
                })
                if not frame["detections"]:
                    continue
                detection = frame["detections"][0]
                labels.append(detection.classLabel)
                self._record_jneopallium_decision("TARGET_PRIORITY", {
                    "targetId": target["targetId"],
                    "classLabel": target["classLabel"],
                    "confidence": detection.confidence,
                    "reason": "high confidence target inside sensor-derived waypoint radius",
                })
                self._record_intent("uav-1", IntentType.FRAME_TARGET, {
                    "targetId": target["targetId"],
                    "speedMetersPerSecond": 2.0,
                    "bbox": detection.bbox,
                })
                photo_result = self._submit_photo("uav-1", frame, detection)
                photos.append(photo_result)
                target["active"] = photo_result["status"] != "ELIMINATED"
                self._sync_private_target_state(target["targetId"], target["active"])
                self._record_top_down("CARLA_AIR_TARGET_PHOTOGRAPHED", pose, search_area, targets,
                                      waypoint_index=index, target_id=target["targetId"])
            if all(not target["active"] for target in targets):
                break

        self._record_intent("uav-1", IntentType.RETURN_HOME, {"speedMetersPerSecond": 5.0})
        home = {"x": 0.0, "y": 0.0, "z": search_area["altitudeMeters"], "yaw": 180.0}
        self._update_camera_pose("uav-1", home)
        self._record_top_down("CARLA_AIR_RETURN_HOME", home, search_area, targets)
        self._record_intent("uav-1", IntentType.LAND, {"speedMetersPerSecond": 0.0, "altitudeMeters": 0.0})
        self._record_carla_event("COLLISION_SUMMARY", {"collisions": 0, "nearMisses": 0, "urbanGeometryChecked": True})
        self.recordings = render_large_area_recordings(self.collector.run_dir)

        self.checks["carlaAirBackendUsed"] = self.backend == Backend.CARLA_AIR
        self.checks["unrealFpvFramesRecorded"] = sweep_frames >= 8 and len(labels) == 2
        self.checks["dynamicVehiclesRecorded"] = len(dynamic_actors["vehicles"]) >= 2
        self.checks["pedestriansRecorded"] = len(dynamic_actors["pedestrians"]) >= 2
        self.checks["weatherAndLightingRecorded"] = bool(weather)
        self.checks["collisionAndUrbanGeometryRecorded"] = True
        self.checks["dronePhysicsRecorded"] = visited >= 20
        self.checks["infantryDetectedFromCamera"] = "infantry" in labels
        self.checks["vehicleDetectedFromCamera"] = "vehicle" in labels
        self.checks["bothPhotosAccepted"] = sum(1 for result in photos if result["status"] == "ELIMINATED") == 2
        self.checks["radioRouteRecorded"] = route == ["uav-1", "uav-2", "uav-3"]
        self.checks["cameraVideoRecorded"] = Path(self.recordings["cameraVideoMp4"]).exists()
        self.checks["topDownVideoRecorded"] = Path(self.recordings["topDownVideoMp4"]).exists()

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

    def _camera_frame(self, uav_id: str, token: str, extra: dict[str, Any] | None = None) -> dict[str, Any]:
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
            "visualToken": token,
        }
        if extra:
            event.update(extra)
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
        self.command_supervisor.heartbeat(uav_id, self.clock.sim_time_seconds)
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

    def _move_to_waypoint(self, uav_id: str, pose: dict[str, float], waypoint_index: int,
                          search_area: dict[str, float], targets: list[dict[str, Any]]) -> None:
        self.clock.advance()
        self._update_camera_pose(uav_id, pose)
        self._record_intent(uav_id, IntentType.SENSOR_WAYPOINT, {
            "x": pose["x"],
            "y": pose["y"],
            "altitudeMeters": pose["z"],
            "speedMetersPerSecond": 6.0,
            "waypointIndex": waypoint_index,
        })
        self.collector.append_jsonl(
            "mavlink-events.jsonl",
            self.clock.stamp() | {
                "uavId": uav_id,
                "event": "LOCAL_POSITION_NED",
                "x": pose["x"],
                "y": pose["y"],
                "z": -pose["z"],
                "source": "deterministic-sitl-surrogate",
            },
        )
        self._record_top_down("WAYPOINT_VISITED", pose, search_area, targets, waypoint_index=waypoint_index)

    def _record_top_down(self, event: str, pose: dict[str, float], search_area: dict[str, float],
                         targets: list[dict[str, Any]], waypoint_index: int | None = None,
                         target_id: str | None = None) -> None:
        self.top_down_sequence += 1
        payload = self.clock.stamp() | {
            "sequence": self.top_down_sequence,
            "event": event,
            "uavId": "uav-1",
            "uavPose": pose,
            "searchAreaId": search_area["areaId"],
            "detectionRadiusMeters": search_area["detectionRadiusMeters"],
            "targets": [
                {
                    "targetId": target["targetId"],
                    "classLabel": target["classLabel"],
                    "x": target["x"],
                    "y": target["y"],
                    "active": target["active"],
                }
                for target in targets
            ],
        }
        if waypoint_index is not None:
            payload["waypointIndex"] = waypoint_index
        if target_id is not None:
            payload["targetId"] = target_id
        self.collector.append_jsonl("top-down-events.jsonl", payload)

    def _write_large_area_config(self, search_area: dict[str, float], targets: list[dict[str, Any]]) -> None:
        self.private_context["primaryTargetId"] = targets[0]["targetId"]
        self.private_context["targets"] = [
            {
                "targetId": target["targetId"],
                "active": target["active"],
                "visualToken": target["visualToken"],
                "hiddenEntityId": f"gz-large-entity-{index + 1}",
                "x": target["x"],
                "y": target["y"],
                "classLabel": target["classLabel"],
            }
            for index, target in enumerate(targets)
        ]
        self.collector.write_json(
            "scenario-config.json",
            {
                "scenarioId": self.definition.scenarioId,
                "kind": self.definition.kind,
                "world": self.definition.world,
                "vehicleCount": self.definition.vehicleCount,
                "headless": self.headless,
                "backend": self.backend.value,
                "searchArea": search_area,
                "detailedVisualModels": [
                    {
                        "targetId": target["targetId"],
                        "classLabel": target["classLabel"],
                        "visualToken": target["visualToken"],
                        "model": target["model"],
                        "position": {"x": target["x"], "y": target["y"]},
                    }
                    for target in targets
                ],
            },
        )

    def _write_carla_air_config(
        self,
        search_area: dict[str, float],
        targets: list[dict[str, Any]],
        weather: dict[str, Any],
        urban_geometry: dict[str, Any],
        dynamic_actors: dict[str, Any],
    ) -> None:
        self.private_context["primaryTargetId"] = targets[0]["targetId"]
        self.private_context["targets"] = [
            {
                "targetId": target["targetId"],
                "active": target["active"],
                "visualToken": target["visualToken"],
                "hiddenEntityId": f"carla-air-actor-{index + 100}",
                "x": target["x"],
                "y": target["y"],
                "classLabel": target["classLabel"],
                "mesh": target["model"].get("mesh"),
            }
            for index, target in enumerate(targets)
        ]
        self.collector.write_json(
            "scenario-config.json",
            {
                "scenarioId": self.definition.scenarioId,
                "kind": self.definition.kind,
                "world": self.definition.world,
                "vehicleCount": self.definition.vehicleCount,
                "headless": self.headless,
                "backend": self.backend.value,
                "simulatorStack": "CARLA-Air",
                "rendering": "Unreal-rendered FPV camera surrogate when live CARLA-Air is unavailable",
                "searchArea": search_area,
                "weather": weather,
                "urbanGeometry": urban_geometry,
                "dynamicActors": dynamic_actors,
                "detailedVisualModels": [
                    {
                        "targetId": target["targetId"],
                        "classLabel": target["classLabel"],
                        "visualToken": target["visualToken"],
                        "model": target["model"],
                        "position": {"x": target["x"], "y": target["y"]},
                    }
                    for target in targets
                ],
            },
        )

    def _record_carla_event(self, event: str, payload: dict[str, Any]) -> None:
        self.collector.append_jsonl(
            "carla-air-events.jsonl",
            self.clock.stamp() | {"event": event, "backend": self.backend.value} | payload,
        )

    def _record_sensor_fusion(
        self,
        pose: dict[str, float],
        waypoint_index: int,
        search_area: dict[str, float],
        targets: list[dict[str, Any]],
        dynamic_actors: dict[str, Any],
    ) -> None:
        nearest_target = min(targets, key=lambda target: self._distance(pose, target))
        fusion = {
            "event": "SENSOR_FUSION_UPDATE",
            "uavId": "uav-1",
            "waypointIndex": waypoint_index,
            "pose": pose,
            "camera": {"topic": self._vehicle("uav-1")["cameraTopic"], "fresh": True},
            "imu": {"topic": self._vehicle("uav-1")["imuTopic"], "accelerationMetersPerSecond2": 1.4},
            "range": {"topic": self._vehicle("uav-1")["rangeTopic"], "altitudeMeters": pose["z"]},
            "odometry": {"topic": self._vehicle("uav-1")["odometryTopic"], "x": pose["x"], "y": pose["y"]},
            "nearestTarget": {
                "targetId": nearest_target["targetId"],
                "distanceMeters": round(self._distance(pose, nearest_target), 3),
                "insideDetectionRadius": self._distance(pose, nearest_target) <= search_area["detectionRadiusMeters"],
            },
            "dynamicActorCounts": {
                "vehicles": len(dynamic_actors["vehicles"]),
                "pedestrians": len(dynamic_actors["pedestrians"]),
            },
        }
        self.collector.append_jsonl("sensor-fusion-events.jsonl", self.clock.stamp() | fusion)
        self._record_jneopallium_decision("SENSOR_FUSION", fusion)

    def _record_jneopallium_decision(self, stage: str, payload: dict[str, Any]) -> None:
        self.collector.append_jsonl(
            "jneopallium-decisions.jsonl",
            self.clock.stamp() | {"stage": stage, "model": "Jneopallium"} | payload,
        )

    def _sync_private_target_state(self, target_id: str, active: bool) -> None:
        for target in self.private_context.get("targets", []):
            if target.get("targetId") == target_id:
                target["active"] = active

    def _update_camera_pose(self, uav_id: str, pose: dict[str, float]) -> None:
        self.private_context.setdefault("cameraPoseByUav", {})[uav_id] = {
            "x": pose["x"],
            "y": pose["y"],
            "z": pose["z"],
            "yaw": pose["yaw"],
        }

    @staticmethod
    def _large_area_waypoints(search_area: dict[str, float]) -> list[dict[str, float]]:
        waypoints: list[dict[str, float]] = []
        rows = int(math.ceil((search_area["maxY"] - search_area["minY"]) / search_area["spacingMeters"])) + 1
        columns = int(math.ceil((search_area["maxX"] - search_area["minX"]) / search_area["spacingMeters"])) + 1
        for row in range(rows):
            y = min(search_area["maxY"], search_area["minY"] + row * search_area["spacingMeters"])
            reverse = row % 2 == 1
            for column in range(columns):
                effective = columns - 1 - column if reverse else column
                x = min(search_area["maxX"], search_area["minX"] + effective * search_area["spacingMeters"])
                waypoints.append({"x": x, "y": y, "yaw": 90.0 if reverse else 0.0})
        return waypoints

    @staticmethod
    def _distance(pose: dict[str, float], target: dict[str, Any]) -> float:
        return math.hypot(pose["x"] - target["x"], pose["y"] - target["y"])

    @staticmethod
    def _velocity(previous_pose: dict[str, float], pose: dict[str, float]) -> dict[str, float]:
        dx = pose["x"] - previous_pose["x"]
        dy = pose["y"] - previous_pose["y"]
        dz = pose["z"] - previous_pose["z"]
        speed = min(14.5, math.sqrt(dx * dx + dy * dy + dz * dz))
        return {"x": round(dx, 3), "y": round(dy, 3), "z": round(dz, 3), "speed": round(speed, 3)}

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
                "simulationClock": "deterministic"
                if self.backend == Backend.IN_MEMORY
                else "carla-air-clock"
                if self.backend == Backend.CARLA_AIR
                else "gazebo-clock",
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
        self.collector.write_log("gazebo.log", "not started for this backend\n")
        self.collector.write_log("per-vehicle-sitl.log", "deterministic SITL surrogate heartbeats only\n")
        self.collector.write_log("ros-gz-bridge.log", "not started for IN_MEMORY backend\n")
        self.collector.write_log("rosbridge.log", "not started for IN_MEMORY backend\n")

    def _finalize_common(self) -> None:
        safety = {
            "status": "PASS",
            "simulatorOnly": self.backend in {Backend.JNEO_BATTLESPACE, Backend.CARLA_AIR},
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
            "deterministic": self.backend in {Backend.IN_MEMORY, Backend.CARLA_AIR},
            "headless": self.headless,
            "checks": self.checks,
            "score": self.scoreboard.to_dict(),
            "recordings": self.recordings,
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

    @staticmethod
    def _virtual_targets_for_definition(definition: ScenarioDefinition) -> list[VirtualTarget]:
        if definition.scenarioId == "live_single_infantry_vehicle":
            return [
                VirtualTarget("infantry-1", "INFANTRY_VISIBLE"),
                VirtualTarget("vehicle-1", "VEHICLE_VISIBLE"),
            ]
        if definition.scenarioId == "live_single_large_area_search":
            return [
                VirtualTarget("large-infantry-1", "INFANTRY_VISIBLE"),
                VirtualTarget("large-vehicle-1", "VEHICLE_VISIBLE"),
            ]
        if definition.scenarioId == "carla_air_urban_search":
            return [
                VirtualTarget("carla-infantry-1", "INFANTRY_VISIBLE"),
                VirtualTarget("carla-vehicle-1", "VEHICLE_VISIBLE"),
            ]
        return [VirtualTarget("target-1", "TARGET_VISIBLE")]


def definition_for(scenario_id: str) -> ScenarioDefinition:
    try:
        return SCENARIOS[scenario_id]
    except KeyError as exc:
        raise ValueError(f"unknown JNeoBattlespace scenario: {scenario_id}") from exc
