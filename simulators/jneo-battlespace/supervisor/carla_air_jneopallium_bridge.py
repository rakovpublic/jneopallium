"""Thin CARLA-Air <-> jneopallium bridge (no policy, no recognition).

This process is *only* an I/O bridge. The model — recognition + autonomous movement +
reinforcement learning — lives in the Java worker
(``UavFpvAutonomyRun``). Three phases (a 1000x1000 m area, 4 vehicles + 4 humans):

  Phase 1  ``--mode pretrain``  spawn the 8 CARLA targets, fly near each, capture labeled FPV
           crops and stream them to the Java recognition trainer
           (``UavFpvAutonomyRun --mode train-recognition``) so the recognition prototypes learn
           real vehicle/human pixels.
  Phase 2  ``--mode bridge``    run the whole loop: stream telemetry (MAVLink) + FPV frames +
           perception (camera UDP) to ``UavFpvAutonomyRun --mode live`` and apply the motor
           commands it returns. The worker keeps training recognition + movement RL.
  Phase 3  ``--mode bridge --record``  same loop, recording the FPV MP4 with a bounding box on
           each recognized target that persists for 2 s (TTL) and saving a photo per target.

Telemetry and motor commands ride MAVLink UDP; FPV frames + perception ride a separate camera UDP
channel (images must not ride the MAVLink flight bus, 12-MAVLINK.md §5). No MAVLink dependency:
a minimal MAVLink v2 codec for the two messages used is included.

Start the Java worker for the matching phase first (it binds the UDP ports and waits).
``--mode mavlink-selftest`` verifies the Python<->Java path without CARLA.
"""
from __future__ import annotations

import argparse
import json
import math
import queue
import socket
import struct
import time
from pathlib import Path


# --------------------------------------------------------------------------- MAVLink v2 codec

_CRC_EXTRA = {33: 104, 253: 83}  # GLOBAL_POSITION_INT, STATUSTEXT
MAGIC_V2 = 0xFD


def _crc_accumulate(byte: int, crc: int) -> int:
    tmp = byte ^ (crc & 0xFF)
    tmp = (tmp ^ (tmp << 4)) & 0xFF
    return ((crc >> 8) ^ (tmp << 8) ^ (tmp << 3) ^ (tmp >> 4)) & 0xFFFF


def _crc(frame_wo_magic: bytes, crc_extra: int) -> int:
    crc = 0xFFFF
    for b in frame_wo_magic:
        crc = _crc_accumulate(b, crc)
    return _crc_accumulate(crc_extra, crc)


def encode_global_position_int(seq: int, sysid: int, compid: int, *, time_boot_ms: int,
                               lat: int, lon: int, alt: int, rel_alt: int,
                               vx: int, vy: int, vz: int, hdg: int) -> bytes:
    payload = struct.pack("<IiiiihhhH", time_boot_ms & 0xFFFFFFFF, lat, lon, alt, rel_alt,
                          vx, vy, vz, hdg & 0xFFFF)
    msgid = 33
    header = struct.pack("<BBBBBB", len(payload), 0, 0, seq & 0xFF, sysid, compid)
    body = header + struct.pack("<I", msgid)[:3] + payload
    return bytes([MAGIC_V2]) + body + struct.pack("<H", _crc(body, _CRC_EXTRA[msgid]))


def decode_statustext(data: bytes):
    if len(data) < 12 or data[0] != MAGIC_V2:
        return None
    plen = data[1]
    msgid = data[7] | (data[8] << 8) | (data[9] << 16)
    if msgid != 253:
        return None
    payload = data[10:10 + plen]
    if not payload:
        return None
    return payload[0], payload[1:].split(b"\x00")[0].decode("ascii", errors="ignore")


# --------------------------------------------------------------------------- scenario (1000x1000 m)

METERS_PER_DEG = 111320.0
# Town10HD is ~400 m across, so the search area is fit to the map (±150 m). The literal
# 1000x1000 m / ±430 m layout needs a larger map (e.g. Town12); set --area-half + scale TARGETS for it.
DEFAULT_AREA_HALF = 250.0
PHOTO_RADIUS = 40.0
INLAND_TOWN_BOUNDS = {"minX": -20.0, "maxX": 130.0, "minY": 15.0, "maxY": 65.0}

# Prefer adult pedestrian meshes that are visually larger/clearer than the first arbitrary
# CARLA walker ids. Missing ids are ignored so the bridge still runs across CarlaAir builds.
PREFERRED_HUMAN_BLUEPRINTS = [
    "walker.pedestrian.0034",
    "walker.pedestrian.0036",
    "walker.pedestrian.0037",
    "walker.pedestrian.0021",
    "walker.pedestrian.0019",
    "walker.pedestrian.0018",
    "walker.pedestrian.0017",
    "walker.pedestrian.0007",
]

INLAND_TARGET_PATTERN = [
    (74.0, 17.0),
    (86.0, 17.0),
    (98.0, 17.0),
    (110.0, 17.0),
    (74.0, 30.0),
    (86.0, 30.0),
    (98.0, 30.0),
    (110.0, 30.0),
]

# (target_id, class_label, kind, offset_x_m, offset_y_m) — 4 vehicles + 4 humans across the area.
TARGETS = [
    ("vehicle-1", "VEHICLE_TO_INSPECT", "vehicle", -110.0, -90.0),
    ("vehicle-2", "VEHICLE_TO_INSPECT", "vehicle", 95.0, -80.0),
    ("vehicle-3", "VEHICLE_TO_INSPECT", "vehicle", 100.0, 95.0),
    ("vehicle-4", "VEHICLE_TO_INSPECT", "vehicle", -95.0, 100.0),
    ("human-1", "HUMAN_PERSONNEL_SIM", "walker", -40.0, -115.0),
    ("human-2", "HUMAN_PERSONNEL_SIM", "walker", 115.0, -25.0),
    ("human-3", "HUMAN_PERSONNEL_SIM", "walker", 35.0, 110.0),
    ("human-4", "HUMAN_PERSONNEL_SIM", "walker", -115.0, 30.0),
]

ACTION_VECTORS = {
    "survey-east": (1.0, 0.0), "survey-west": (-1.0, 0.0),
    "survey-north": (0.0, 1.0), "survey-south": (0.0, -1.0),
    "survey-north-east": (0.707, 0.707), "survey-north-west": (-0.707, 0.707),
    "survey-south-east": (0.707, -0.707), "survey-south-west": (-0.707, -0.707),
    "inspect-east": (1.0, 0.0), "inspect-west": (-1.0, 0.0),
    "inspect-north": (0.0, 1.0), "inspect-south": (0.0, -1.0),
    "climb-east": (1.0, 0.0), "climb-west": (-1.0, 0.0),
    "climb-north": (0.0, 1.0), "climb-south": (0.0, -1.0),
}

NON_BLOCKING_LABELS = {
    "Roads", "Sidewalks", "Ground", "Terrain", "RoadLines",
    "Vehicles", "Pedestrians",
}


def local_to_global(x_m: float, y_m: float):
    return int(round((x_m / METERS_PER_DEG) * 1e7)), int(round((y_m / METERS_PER_DEG) * 1e7))


def search_bounds(bounds: dict | None = None) -> dict:
    if bounds:
        return bounds
    half = DEFAULT_AREA_HALF
    return {"minX": -half, "maxX": half, "minY": -half, "maxY": half}


def exact_area_bounds(area_half: float) -> dict:
    half = float(area_half)
    return {"minX": -half, "maxX": half, "minY": -half, "maxY": half}


def town_center_start() -> tuple[float, float]:
    if INLAND_TARGET_PATTERN:
        avg_x = sum(point[0] for point in INLAND_TARGET_PATTERN) / len(INLAND_TARGET_PATTERN)
        avg_y = sum(point[1] for point in INLAND_TARGET_PATTERN) / len(INLAND_TARGET_PATTERN)
        return avg_x, -avg_y
    return (
        (INLAND_TOWN_BOUNDS["minX"] + INLAND_TOWN_BOUNDS["maxX"]) / 2.0,
        -((INLAND_TOWN_BOUNDS["minY"] + INLAND_TOWN_BOUNDS["maxY"]) / 2.0),
    )


def _carla_to_airsim_offset(origin, location) -> tuple[float, float]:
    """CARLA and AirSim share X in this build, but their local Y axes are mirrored."""
    return location.x - origin.x, -(location.y - origin.y)


# --------------------------------------------------------------------------- CARLA spawning

def _inside_inland_town(ox: float, oy: float) -> bool:
    return INLAND_TOWN_BOUNDS["minX"] <= ox <= INLAND_TOWN_BOUNDS["maxX"] \
        and INLAND_TOWN_BOUNDS["minY"] <= oy <= INLAND_TOWN_BOUNDS["maxY"]


def _has_overhead_clearance(world, location, altitude: float = 120.0) -> bool:
    import carla  # noqa: WPS433

    start = carla.Location(x=location.x, y=location.y, z=location.z + altitude)
    end = carla.Location(x=location.x, y=location.y, z=location.z + 1.5)
    total = max(1.0, start.distance(end))
    try:
        hits = world.cast_ray(start, end)
    except Exception:  # noqa: BLE001
        return True
    for hit in hits:
        if str(hit.label) in NON_BLOCKING_LABELS:
            continue
        if _hit_distance(start, hit) < total - 2.0:
            return False
    return True


def _spawn_candidates(world, origin, area_half: float, town_only: bool):
    spawn_points = list(world.get_map().get_spawn_points())
    candidates = []
    for index, transform in enumerate(spawn_points):
        ox = transform.location.x - origin.x
        oy = transform.location.y - origin.y
        distance = math.hypot(ox, oy)
        if distance > area_half * 1.08:
            continue
        if town_only and not _inside_inland_town(ox, oy):
            continue
        if not _has_overhead_clearance(world, transform.location):
            continue
        if distance >= 12.0:
            candidates.append((index, distance, ox, oy, transform))
    return candidates


def _preferred_human_blueprints(library):
    by_id = {blueprint.id: blueprint for blueprint in library.filter("walker.pedestrian.*")}
    preferred = [by_id[item] for item in PREFERRED_HUMAN_BLUEPRINTS if item in by_id]
    fallback = [blueprint for blueprint in by_id.values() if blueprint not in preferred]
    return preferred + fallback


def _prepare_human_blueprint(blueprint, target_id: str):
    if blueprint.has_attribute("role_name"):
        blueprint.set_attribute("role_name", target_id)
    if blueprint.has_attribute("is_invincible"):
        blueprint.set_attribute("is_invincible", "false")
    return blueprint


def _stable_actor_location(actor, fallback):
    if actor is None:
        return fallback
    try:
        loc = actor.get_location()
        if loc is not None and loc.distance(fallback) < 12.0:
            return loc
    except Exception:  # noqa: BLE001
        pass
    return fallback


def _spread_spawn_points(world, origin, area_half: float, town_only: bool):
    candidates = _spawn_candidates(world, origin, area_half, town_only)
    if len(candidates) < len(TARGETS) and town_only:
        print(f"[carla] inland town filter returned {len(candidates)} spawn points; "
              "falling back to full town road graph")
        candidates = _spawn_candidates(world, origin, area_half, False)
    selected = []
    used = set()
    for target_index, _target in enumerate(TARGETS):
        wanted_x, wanted_y = INLAND_TARGET_PATTERN[target_index % len(INLAND_TARGET_PATTERN)]
        wanted_angle = math.atan2(wanted_y, wanted_x)
        best = None
        best_score = float("inf")
        for index, distance, ox, oy, transform in candidates:
            if index in used:
                continue
            angle = math.atan2(oy, ox)
            angle_delta = abs(math.atan2(math.sin(angle - wanted_angle), math.cos(angle - wanted_angle)))
            desired_delta = math.hypot(ox - wanted_x, oy - wanted_y)
            too_close_penalty = 0.0
            for used_x, used_y, _used_transform in selected:
                separation = math.hypot(ox - used_x, oy - used_y)
                if separation < 14.0:
                    too_close_penalty += (14.0 - separation) * 8.0
            score = desired_delta + angle_delta * 8.0 + too_close_penalty
            if score < best_score:
                best = (index, ox, oy, transform)
                best_score = score
        if best is not None:
            used.add(best[0])
            selected.append(best[1:])
    return selected


def _bounds_from_targets(targets, margin: float) -> dict:
    xs = [target["x"] for target in targets]
    ys = [target["y"] for target in targets]
    if not xs or not ys:
        return search_bounds()
    return {
        "minX": math.floor(min(xs) - margin),
        "maxX": math.ceil(max(xs) + margin),
        "minY": math.floor(min(ys) - margin),
        "maxY": math.ceil(max(ys) + margin),
    }


def _bounds_from_spawn_points(world, origin, area_half: float, town_only: bool, margin: float = 8.0) -> dict:
    offsets = []
    for _index, _distance, ox, oy, _transform in _spawn_candidates(world, origin, area_half, town_only):
        offsets.append((ox, -oy))
    if not offsets:
        return search_bounds()
    xs = [item[0] for item in offsets]
    ys = [item[1] for item in offsets]
    return {
        "minX": math.floor(min(xs) - margin),
        "maxX": math.ceil(max(xs) + margin),
        "minY": math.floor(min(ys) - margin),
        "maxY": math.ceil(max(ys) + margin),
    }


def spawn_targets(world, origin, area_half: float = DEFAULT_AREA_HALF, town_only: bool = True,
                  exact_search_area: bool = True):
    """Spawn targets on CARLA town spawn points. Returns (targets, town_bounds)."""
    import carla  # noqa: WPS433

    library = world.get_blueprint_library()
    vehicles = list(library.filter("vehicle.*"))
    walkers = _preferred_human_blueprints(library)
    if town_only:
        selected = [
            (ox, oy, carla.Transform(carla.Location(x=origin.x + ox, y=origin.y + oy, z=origin.z)))
            for ox, oy in INLAND_TARGET_PATTERN
        ]
    else:
        selected = _spread_spawn_points(world, origin, area_half, town_only)
    spawned = []
    for idx, (target_id, label, kind, fallback_ox, fallback_oy) in enumerate(TARGETS):
        if idx < len(selected):
            ox, oy, selected_transform = selected[idx]
            location = carla.Location(x=origin.x + ox, y=origin.y + oy, z=selected_transform.location.z)
        else:
            ox, oy = fallback_ox, fallback_oy
            selected_transform = carla.Transform(carla.Location(x=origin.x + ox, y=origin.y + oy, z=origin.z))
            location = selected_transform.location
        if kind == "vehicle":
            waypoint = world.get_map().get_waypoint(location, project_to_road=True)
            transform = waypoint.transform if waypoint else selected_transform
            transform.location.z += 0.3
            blueprint = vehicles[idx % len(vehicles)]
        else:
            waypoint = world.get_map().get_waypoint(location, project_to_road=True)
            base = waypoint.transform.location if waypoint else location
            yaw = math.radians((waypoint.transform.rotation.yaw if waypoint else selected_transform.rotation.yaw) + 90.0)
            transform = carla.Transform(
                carla.Location(x=base.x + math.cos(yaw) * 2.0, y=base.y + math.sin(yaw) * 2.0, z=base.z + 1.0))
            blueprint = _prepare_human_blueprint(walkers[idx % len(walkers)], target_id)
        if blueprint.has_attribute("role_name"):
            blueprint.set_attribute("role_name", target_id)
        spawn_transform = transform
        actor = world.try_spawn_actor(blueprint, spawn_transform)
        if actor is None:
            spawn_transform = carla.Transform(location)
            actor = world.try_spawn_actor(blueprint, spawn_transform)
        if actor is None and kind == "walker":
            for alternate in walkers:
                blueprint = _prepare_human_blueprint(alternate, target_id)
                actor = world.try_spawn_actor(blueprint, spawn_transform)
                if actor is not None:
                    break
        if actor is None:
            print(f"[carla] skipped {target_id} ({label}): no actor could be spawned")
            continue
        actual = _stable_actor_location(actor, spawn_transform.location)
        actual_ox = actual.x - origin.x
        actual_oy = actual.y - origin.y
        actual_ax, actual_ay = _carla_to_airsim_offset(origin, actual)
        # Drive search/projection from CARLA's final actor position, not the requested spawn
        # point. Waypoint projection can nudge actors enough to make FPV boxes look off-target.
        spawned.append({"id": target_id, "label": label, "kind": kind, "actor": actor,
                        "x": actual_ax, "y": actual_ay,
                        "carlaX": actual_ox, "carlaY": actual_oy,
                        "worldLocation": actual})
        print(f"[carla] spawned {target_id} ({label}) at CARLA offset "
              f"({actual_ox:.0f},{actual_oy:.0f}); AirSim offset ({actual_ax:.0f},{actual_ay:.0f})")
    bounds = exact_area_bounds(area_half) if exact_search_area \
        else _bounds_from_spawn_points(world, origin, area_half, town_only)
    width = bounds["maxX"] - bounds["minX"]
    height = bounds["maxY"] - bounds["minY"]
    placement = "inland town" if town_only else "full town"
    mode = "exact requested" if exact_search_area else "town-fitted"
    print(f"[carla] {placement} search bounds ({mode}) {width:.0f}x{height:.0f} m: {bounds}")
    return spawned, bounds


def project_bbox(target, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg):
    """Approximate pinhole projection of a target offset into the FPV image. Returns (cx,cy,w,h) or None."""
    import numpy as np  # noqa: WPS433

    dx = target["x"] - drone_pos[0]
    dy = target["y"] - drone_pos[1]
    target_z = -_target_projection_height(target)
    dz = target_z - drone_pos[2]
    yaw = math.radians(yaw_deg)
    pitch = math.radians(pitch_deg)
    # rotate world delta into camera frame (yaw then pitch); camera +z forward, +x right, +y down.
    fx = dx * math.cos(yaw) + dy * math.sin(yaw)
    fy = -dx * math.sin(yaw) + dy * math.cos(yaw)
    forward = fx * math.cos(pitch) - dz * math.sin(pitch)
    down = fx * math.sin(pitch) + dz * math.cos(pitch)
    if forward <= 1.0:
        return None
    focal = (width / 2.0) / math.tan(math.radians(fov_deg) / 2.0)
    cx = width / 2.0 + focal * (fy / forward)
    cy = height / 2.0 - focal * (down / forward)
    distance = math.sqrt(fx * fx + fy * fy + dz * dz)
    extent = _target_projection_extent(target)
    box = max(12.0, focal * extent / max(1.0, distance))
    if cx < 0 or cx > width or cy < 0 or cy > height:
        return None
    return int(cx), int(cy), int(box), int(box * (1.4 if target["kind"] == "walker" else 0.8))


def _quat_to_rotation_matrix(quat):
    import numpy as np  # noqa: WPS433

    w, x, y, z = quat.w_val, quat.x_val, quat.y_val, quat.z_val
    return np.array([
        [1.0 - 2.0 * (y * y + z * z), 2.0 * (x * y - z * w), 2.0 * (x * z + y * w)],
        [2.0 * (x * y + z * w), 1.0 - 2.0 * (x * x + z * z), 2.0 * (y * z - x * w)],
        [2.0 * (x * z - y * w), 2.0 * (y * z + x * w), 1.0 - 2.0 * (x * x + y * y)],
    ], dtype=float)


def _target_projection_height(target) -> float:
    return 1.1 if target["kind"] == "walker" else 0.8


def _target_projection_extent(target) -> float:
    return 4.5 if target["kind"] == "vehicle" else 3.4


def project_bbox_from_camera_pose(target, origin, camera_pose, width: int, height: int, fov_deg: float):
    """Project an AirSim-space target using the rendered camera pose instead of estimated yaw/pitch."""
    if camera_pose is None:
        return None
    import numpy as np  # noqa: WPS433

    camera = np.array([
        camera_pose.position.x_val,
        camera_pose.position.y_val,
        camera_pose.position.z_val,
    ], dtype=float)
    target_position = np.array([
        origin.x_val + target["x"],
        origin.y_val + target["y"],
        -_target_projection_height(target),
    ], dtype=float)
    camera_from_world = _quat_to_rotation_matrix(camera_pose.orientation).T
    vector = camera_from_world @ (target_position - camera)
    forward = vector[0]
    if forward <= 1.0:
        return None
    focal = (width / 2.0) / math.tan(math.radians(fov_deg) / 2.0)
    cx = width / 2.0 + focal * (vector[1] / forward)
    cy = height / 2.0 - focal * (vector[2] / forward)
    distance = float(np.linalg.norm(vector))
    extent = _target_projection_extent(target)
    box = max(12.0, focal * extent / max(1.0, distance))
    if cx < 0 or cx > width or cy < 0 or cy > height:
        return None
    return int(cx), int(cy), int(box), int(box * (1.4 if target["kind"] == "walker" else 0.8))


def target_boxes(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg):
    boxes = []
    pose_box = project_bbox_from_camera_pose(target, origin, camera_pose, width, height, fov_deg)
    fallback_box = project_bbox(target, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg)
    for box in (pose_box, fallback_box):
        if box is not None and box not in boxes:
            boxes.append(box)
    return boxes


def target_box(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg):
    boxes = target_boxes(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg)
    return boxes[0] if boxes else None


def decode_airsim_scene_bgr(response, width: int, height: int):
    import cv2  # noqa: WPS433
    import numpy as np  # noqa: WPS433

    rgb = np.frombuffer(response.image_data_uint8, dtype=np.uint8).reshape(response.height, response.width, 3)
    return cv2.cvtColor(cv2.resize(rgb, (width, height)), cv2.COLOR_RGB2BGR)


class CarlaRgbCamera:
    def __init__(self, world, width: int, height: int, fov_deg: float, fps: float, transform):
        import carla  # noqa: WPS433

        self.width = width
        self.height = height
        self.frames: queue.Queue = queue.Queue(maxsize=4)
        blueprint = world.get_blueprint_library().find("sensor.camera.rgb")
        blueprint.set_attribute("image_size_x", str(width))
        blueprint.set_attribute("image_size_y", str(height))
        blueprint.set_attribute("fov", str(fov_deg))
        blueprint.set_attribute("sensor_tick", str(1.0 / max(1.0, fps)))
        self.actor = world.spawn_actor(blueprint, transform)

        def _on_image(image):
            import numpy as np  # noqa: WPS433

            bgr = np.frombuffer(image.raw_data, dtype=np.uint8).reshape((image.height, image.width, 4))[:, :, :3]
            frame = bgr.copy()
            while self.frames.full():
                try:
                    self.frames.get_nowait()
                except queue.Empty:
                    break
            try:
                self.frames.put_nowait(frame)
            except queue.Full:
                pass

        self.actor.listen(_on_image)

    def read(self, transform, timeout: float = 1.5):
        import cv2  # noqa: WPS433

        while True:
            try:
                self.frames.get_nowait()
            except queue.Empty:
                break
        self.actor.set_transform(transform)
        try:
            frame = self.frames.get(timeout=timeout)
        except queue.Empty:
            return None
        while True:
            try:
                frame = self.frames.get_nowait()
            except queue.Empty:
                break
        if frame.shape[1] != self.width or frame.shape[0] != self.height:
            frame = cv2.resize(frame, (self.width, self.height))
        return frame

    def destroy(self):
        try:
            self.actor.stop()
            self.actor.destroy()
        except Exception:  # noqa: BLE001
            pass


def carla_camera_transform(carla_origin, drone_offset: tuple[float, float, float], altitude_m: float,
                           yaw_deg: float, pitch_deg: float, yaw_offset: float):
    import carla  # noqa: WPS433

    location = carla.Location(
        x=carla_origin.x + drone_offset[0],
        y=carla_origin.y - drone_offset[1],
        z=max(2.0, altitude_m),
    )
    return carla.Transform(
        location,
        carla.Rotation(pitch=pitch_deg, yaw=-(yaw_deg + yaw_offset), roll=0.0),
    )


def _project_carla_point(world_point, camera_transform, width: int, height: int, fov_deg: float):
    import numpy as np  # noqa: WPS433

    world_2_camera = np.array(camera_transform.get_inverse_matrix())
    point = np.array([world_point.x, world_point.y, world_point.z, 1.0])
    camera_point = world_2_camera @ point
    # CARLA camera coordinates are x-forward, y-right, z-up. Convert to
    # conventional image coordinates: x-right, y-down, z-forward.
    right = camera_point[1]
    down = -camera_point[2]
    forward = camera_point[0]
    if forward <= 0.15:
        return None
    focal = (width / 2.0) / math.tan(math.radians(fov_deg) / 2.0)
    x = width / 2.0 + focal * right / forward
    y = height / 2.0 + focal * down / forward
    return x, y, forward


def project_actor_bbox(target, camera_transform, width: int, height: int, fov_deg: float):
    actor = target.get("actor")
    if actor is None:
        return None
    try:
        vertices = actor.bounding_box.get_world_vertices(actor.get_transform())
    except Exception:  # noqa: BLE001
        return None
    points = [_project_carla_point(vertex, camera_transform, width, height, fov_deg) for vertex in vertices]
    points = [point for point in points if point is not None]
    if len(points) < 2:
        return None
    xs = [point[0] for point in points]
    ys = [point[1] for point in points]
    min_x = max(0.0, min(xs))
    max_x = min(float(width - 1), max(xs))
    min_y = max(0.0, min(ys))
    max_y = min(float(height - 1), max(ys))
    if max_x <= min_x or max_y <= min_y:
        return None
    pad = 0.18
    bw = max_x - min_x
    bh = max_y - min_y
    min_x = max(0.0, min_x - bw * pad)
    max_x = min(float(width - 1), max_x + bw * pad)
    min_y = max(0.0, min_y - bh * pad)
    max_y = min(float(height - 1), max_y + bh * pad)
    bw = max(10.0, max_x - min_x)
    bh = max(10.0, max_y - min_y)
    cx = (min_x + max_x) / 2.0
    cy = (min_y + max_y) / 2.0
    if cx < 0 or cx > width or cy < 0 or cy > height:
        return None
    return int(round(cx)), int(round(cy)), int(round(bw)), int(round(bh))


def target_boxes(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg,
                 carla_camera_transform=None):
    boxes = []
    if carla_camera_transform is not None:
        actor_box = project_actor_bbox(target, carla_camera_transform, width, height, fov_deg)
        if actor_box is not None:
            boxes.append(actor_box)
            return boxes
    pose_box = project_bbox_from_camera_pose(target, origin, camera_pose, width, height, fov_deg)
    fallback_box = project_bbox(target, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg)
    for box in (pose_box, fallback_box):
        if box is not None and box not in boxes:
            boxes.append(box)
    return boxes


def java_class(label: str | None) -> str | None:
    upper = (label or "").upper()
    if "HUMAN" in upper or "PERSON" in upper or "INFANTRY" in upper or "WALKER" in upper:
        return "INFANTRY"
    if "VEHICLE" in upper or "CAR" in upper or "TRUCK" in upper:
        return "VEHICLE_TO_INSPECT"
    return label


def expanded_box(box, width: int, height: int, pad: float = 2.4, min_half: int = 18):
    if box is None:
        return None
    cx, cy, bw, bh = box
    half_w = max(min_half, int((bw * pad) / 2))
    half_h = max(min_half, int((bh * pad) / 2))
    x1 = max(0, cx - half_w)
    y1 = max(0, cy - half_h)
    x2 = min(width, cx + half_w)
    y2 = min(height, cy + half_h)
    if x2 <= x1 or y2 <= y1:
        return None
    return x1, y1, x2, y2


def usable_projected_box(box, width: int, height: int) -> bool:
    if box is None:
        return False
    cx, cy, bw, bh = box
    if bw < 10 or bh < 10:
        return False
    return (cx - bw // 2) >= 0 and (cy - bh // 2) >= 0 \
        and (cx + bw // 2) < width and (cy + bh // 2) < height


def _box_context_metrics(bgr, box):
    import cv2  # noqa: WPS433
    import numpy as np  # noqa: WPS433

    if box is None:
        return None
    height, width = bgr.shape[:2]
    expanded = expanded_box(box, width, height, pad=3.0)
    if expanded is None:
        return None
    x1, y1, x2, y2 = expanded
    crop = bgr[y1:y2, x1:x2]
    if crop.size == 0:
        return None
    hsv = cv2.cvtColor(crop, cv2.COLOR_BGR2HSV)
    hue = hsv[:, :, 0]
    saturation = hsv[:, :, 1]
    value = hsv[:, :, 2]
    b, g, r = cv2.split(crop)
    blue_gray = (b.astype(int) > r.astype(int) + 6) & (g.astype(int) > r.astype(int) + 2)
    blue_hue = (hue >= 82) & (hue <= 116) & (saturation >= 10) & (value >= 70)
    water_ratio = float(np.mean(blue_gray | blue_hue))
    sand_hue = (hue >= 10) & (hue <= 32) & (saturation <= 90) & (value >= 130)
    pale_low_detail = (saturation <= 35) & (value >= 170)
    sand_ratio = float(np.mean(sand_hue | pale_low_detail))
    cx1 = crop.shape[1] // 3
    cx2 = max(cx1 + 1, crop.shape[1] - cx1)
    cy1 = crop.shape[0] // 3
    cy2 = max(cy1 + 1, crop.shape[0] - cy1)
    center_water_ratio = float(np.mean((blue_gray | blue_hue)[cy1:cy2, cx1:cx2]))
    gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
    texture = cv2.Laplacian(gray, cv2.CV_64F).var()
    edges = cv2.Canny(gray, 60, 160)
    edge_ratio = float(np.mean(edges > 0))
    dark_object_ratio = float(np.mean((gray < 95) & (saturation > 18)))
    return {
        "waterRatio": water_ratio,
        "centerWaterRatio": center_water_ratio,
        "sandRatio": sand_ratio,
        "texture": texture,
        "edgeRatio": edge_ratio,
        "darkObjectRatio": dark_object_ratio,
    }


def _frame_water_ratio(bgr) -> float:
    import cv2  # noqa: WPS433
    import numpy as np  # noqa: WPS433

    if bgr is None or bgr.size == 0:
        return 1.0
    sample = cv2.resize(bgr, (160, 90))
    hsv = cv2.cvtColor(sample, cv2.COLOR_BGR2HSV)
    hue = hsv[:, :, 0]
    saturation = hsv[:, :, 1]
    value = hsv[:, :, 2]
    blue_hue = (hue >= 82) & (hue <= 116) & (saturation >= 10) & (value >= 70)
    b, g, r = cv2.split(sample)
    blue_gray = (b.astype(int) > r.astype(int) + 6) & (g.astype(int) > r.astype(int) + 2)
    return float(np.mean(blue_gray | blue_hue))


def box_context_score(bgr, box) -> float:
    metrics = _box_context_metrics(bgr, box)
    if metrics is None:
        return -10.0
    water = metrics["waterRatio"]
    center_water = metrics["centerWaterRatio"]
    sand = metrics["sandRatio"]
    texture = metrics["texture"]
    edge_ratio = metrics["edgeRatio"]
    dark_ratio = metrics["darkObjectRatio"]
    score = min(texture / 420.0, 2.2) + min(edge_ratio * 18.0, 1.6) + min(dark_ratio * 10.0, 1.2)
    score -= water * 3.0 + center_water * 4.0
    if texture < 340.0:
        score -= sand * 1.4
    return score


def box_has_town_context(bgr, box) -> bool:
    """Reject projected boxes that land on open water/beach instead of town geometry."""
    metrics = _box_context_metrics(bgr, box)
    if metrics is None:
        return False
    # Water/surf in Town10HD is blue-gray; beach/surf is pale and low-detail.
    if metrics["centerWaterRatio"] > 0.18 or metrics["waterRatio"] > 0.26:
        return False
    if metrics["waterRatio"] > 0.28 and metrics["texture"] < 380.0:
        return False
    if metrics["sandRatio"] > 0.72 and metrics["texture"] < 260.0 and metrics["edgeRatio"] < 0.055:
        return False
    return box_context_score(bgr, box) > 0.35


def _target_evidence_metrics(bgr, box):
    import cv2  # noqa: WPS433
    import numpy as np  # noqa: WPS433

    if box is None:
        return None
    height, width = bgr.shape[:2]
    raw = expanded_box(box, width, height, pad=1.15, min_half=5)
    if raw is None:
        return None
    x1, y1, x2, y2 = raw
    crop = bgr[y1:y2, x1:x2]
    if crop.size == 0:
        return None
    gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
    hsv = cv2.cvtColor(crop, cv2.COLOR_BGR2HSV)
    edges = cv2.Canny(gray, 45, 135)
    texture = float(cv2.Laplacian(gray, cv2.CV_64F).var())

    border = np.concatenate([
        gray[0:2, :].reshape(-1), gray[-2:, :].reshape(-1),
        gray[:, 0:2].reshape(-1), gray[:, -2:].reshape(-1),
    ])
    background = float(np.median(border)) if border.size else float(np.median(gray))
    color_background = np.median(crop.reshape(-1, 3), axis=0)
    gray_delta = np.abs(gray.astype(float) - background)
    color_delta = np.linalg.norm(crop.astype(float) - color_background, axis=2)
    saturation = hsv[:, :, 1]
    foreground = (gray_delta > 18.0) | (color_delta > 35.0) | (edges > 0) | ((gray < 95) & (saturation > 16))
    fg_ratio = float(np.mean(foreground))
    edge_ratio = float(np.mean(edges > 0))

    cy1 = crop.shape[0] // 3
    cy2 = max(cy1 + 1, crop.shape[0] - cy1)
    cx1 = crop.shape[1] // 3
    cx2 = max(cx1 + 1, crop.shape[1] - cx1)
    center_fg_ratio = float(np.mean(foreground[cy1:cy2, cx1:cx2]))
    center_edge_ratio = float(np.mean((edges > 0)[cy1:cy2, cx1:cx2]))

    mask = foreground.astype("uint8") * 255
    contours, _hierarchy = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    contour_area_ratio = 0.0
    contour_aspect = 0.0
    if contours:
        contour = max(contours, key=cv2.contourArea)
        area = float(cv2.contourArea(contour))
        bx, by, bw, bh = cv2.boundingRect(contour)
        contour_area_ratio = area / max(1.0, crop.shape[0] * crop.shape[1])
        contour_aspect = bw / max(1.0, bh)

    _cx, _cy, bw, bh = box
    return {
        "targetTexture": texture,
        "targetEdgeRatio": edge_ratio,
        "targetForegroundRatio": fg_ratio,
        "targetCenterForegroundRatio": center_fg_ratio,
        "targetCenterEdgeRatio": center_edge_ratio,
        "targetContourAreaRatio": contour_area_ratio,
        "targetContourAspect": contour_aspect,
        "projectedBoxAspect": bw / max(1.0, bh),
    }


def evaluate_target_evidence(target, bgr, box, args):
    if not args.strict_target_evidence:
        return True, "DISABLED", {}
    context = _box_context_metrics(bgr, box)
    metrics = _target_evidence_metrics(bgr, box)
    if context is None or metrics is None:
        return False, "NO_EVALUABLE_BOX", {}
    frame_water_ratio = _frame_water_ratio(bgr)
    combined = {**context, **metrics, "frameWaterRatio": frame_water_ratio}
    actor_box_mode = getattr(args, "camera_source", "airsim") == "carla-rgb"
    if not actor_box_mode:
        if frame_water_ratio > args.max_frame_water_ratio:
            return False, "WATER_HEAVY_FRAME", combined
        if not box_has_town_context(bgr, box):
            return False, "WATER_OR_LOW_TOWN_CONTEXT", combined
        if context["centerWaterRatio"] > args.max_target_center_water_ratio:
            return False, "WATER_CONTEXT", combined
    has_edges = metrics["targetEdgeRatio"] >= args.min_target_edge_ratio \
        or metrics["targetCenterEdgeRatio"] >= args.min_target_center_edge_ratio
    has_foreground = metrics["targetForegroundRatio"] >= args.min_target_foreground_ratio \
        or metrics["targetCenterForegroundRatio"] >= args.min_target_center_foreground_ratio
    has_texture = metrics["targetTexture"] >= args.min_target_texture
    if not (has_edges and has_foreground) and not (has_texture and has_foreground):
        return False, "EMPTY_BOX_LOW_OBJECT_EVIDENCE", combined
    if not actor_box_mode:
        aspect = metrics["projectedBoxAspect"]
        if target["kind"] == "vehicle" and aspect < args.min_vehicle_box_aspect:
            return False, "SHAPE_MISMATCH_VEHICLE_NOT_WIDE", combined
        if target["kind"] == "walker" and aspect > args.max_walker_box_aspect:
            return False, "SHAPE_MISMATCH_WALKER_NOT_TALL", combined
    return True, "TARGET_EVIDENCE_OK", combined


def context_adjusted_box(bgr, box, width: int, height: int):
    if not usable_projected_box(box, width, height):
        return None
    cx, cy, bw, bh = box
    candidates = [box, (cx, height - cy, bw, bh)]
    scored = []
    for candidate in candidates:
        if usable_projected_box(candidate, width, height):
            scored.append((box_context_score(bgr, candidate), candidate))
    if not scored:
        return None
    score, selected = max(scored, key=lambda item: item[0])
    if score <= 0.35 or not box_has_town_context(bgr, selected):
        return None
    return selected


def visible_context_box(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg, bgr,
                        carla_camera_transform=None):
    if carla_camera_transform is not None:
        for box in target_boxes(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg,
                                carla_camera_transform):
            if usable_projected_box(box, width, height):
                return box
        return None
    scored = []
    for box in target_boxes(target, origin, camera_pose, drone_pos, yaw_deg, pitch_deg, width, height, fov_deg,
                            carla_camera_transform):
        adjusted = context_adjusted_box(bgr, box, width, height)
        if adjusted is not None:
            scored.append((box_context_score(bgr, adjusted), adjusted))
    if not scored:
        return None
    return max(scored, key=lambda item: item[0])[1]


def crop_gray_pixels(bgr, box, downsample: int, pad: float = 2.4, min_half: int = 18):
    import cv2  # noqa: WPS433

    h, w = bgr.shape[:2]
    crop_box = expanded_box(box, w, h, pad, min_half)
    if crop_box is None:
        crop = bgr
    else:
        x1, y1, x2, y2 = crop_box
        crop = bgr[y1:y2, x1:x2]
    gray = cv2.cvtColor(cv2.resize(crop, (downsample, downsample)), cv2.COLOR_BGR2GRAY)
    return gray


def target_colour(target):
    return (60, 220, 60) if target["kind"] == "vehicle" else (60, 160, 255)


def annotate_target(frame, target, box, text_suffix: str = ""):
    import cv2  # noqa: WPS433

    if box is None:
        return frame
    cx, cy, bw, bh = box
    x1 = max(0, cx - bw // 2)
    y1 = max(0, cy - bh // 2)
    x2 = min(frame.shape[1] - 1, cx + bw // 2)
    y2 = min(frame.shape[0] - 1, cy + bh // 2)
    colour = target_colour(target)
    cv2.rectangle(frame, (x1, y1), (x2, y2), colour, 2)
    label = f"{target['id']} {java_class(target['label'])}{text_suffix}"
    cv2.putText(frame, label, (x1, max(16, y1 - 6)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, colour, 1, cv2.LINE_AA)
    return frame


def estimate_camera_footprint(altitude_m: float, fov_deg: float, pitch_deg: float) -> float:
    altitude = max(1.0, altitude_m)
    half_fov = math.radians(max(20.0, min(140.0, fov_deg)) / 2.0)
    pitch_factor = max(0.35, math.sin(math.radians(max(10.0, abs(pitch_deg)))))
    return max(PHOTO_RADIUS, min(140.0, altitude * math.tan(half_fov) / pitch_factor))


def render_topdown_frame(bounds: dict, targets: list[dict], drone_offset: tuple[float, float, float],
                         yaw_deg: float, trail: list[tuple[float, float]], saved_ids: set[str],
                         photographed: dict[str, float], now: float, lidar_risk: dict,
                         frame_index: int):
    import cv2  # noqa: WPS433
    import numpy as np  # noqa: WPS433

    canvas_w, canvas_h = 960, 720
    pad = 54
    canvas = np.full((canvas_h, canvas_w, 3), (28, 32, 34), dtype=np.uint8)
    min_x, max_x = bounds["minX"], bounds["maxX"]
    min_y, max_y = bounds["minY"], bounds["maxY"]
    scale = min((canvas_w - pad * 2) / max(1.0, max_x - min_x),
                (canvas_h - pad * 2) / max(1.0, max_y - min_y))

    def pt(x: float, y: float):
        sx = int(round(pad + (x - min_x) * scale))
        sy = int(round(canvas_h - pad - (y - min_y) * scale))
        return sx, sy

    cv2.rectangle(canvas, pt(min_x, min_y), pt(max_x, max_y), (60, 72, 78), 2)
    grid = 35.0
    gx = math.ceil(min_x / grid) * grid
    while gx <= max_x:
        x1, y1 = pt(gx, min_y)
        x2, y2 = pt(gx, max_y)
        cv2.line(canvas, (x1, y1), (x2, y2), (42, 48, 52), 1)
        gx += grid
    gy = math.ceil(min_y / grid) * grid
    while gy <= max_y:
        x1, y1 = pt(min_x, gy)
        x2, y2 = pt(max_x, gy)
        cv2.line(canvas, (x1, y1), (x2, y2), (42, 48, 52), 1)
        gy += grid

    if len(trail) > 1:
        points = np.array([pt(x, y) for x, y in trail[-1200:]], dtype=np.int32)
        cv2.polylines(canvas, [points], False, (80, 190, 255), 2, cv2.LINE_AA)

    for target in targets:
        x, y = pt(target["x"], target["y"])
        colour = target_colour(target)
        hit = target["id"] in saved_ids
        radius = 8 if target["kind"] == "vehicle" else 6
        cv2.circle(canvas, (x, y), radius + (3 if hit else 0), colour, 2 if hit else -1, cv2.LINE_AA)
        if photographed.get(target["id"], 0.0) >= now:
            cv2.circle(canvas, (x, y), radius + 8, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(canvas, target["id"], (x + 10, y - 8), cv2.FONT_HERSHEY_SIMPLEX,
                    0.45, colour, 1, cv2.LINE_AA)

    dx, dy, _ = drone_offset
    drone = pt(dx, dy)
    heading = math.radians(yaw_deg)
    nose = (int(drone[0] + math.cos(heading) * 20), int(drone[1] - math.sin(heading) * 20))
    cv2.circle(canvas, drone, 8, (255, 255, 255), -1, cv2.LINE_AA)
    cv2.line(canvas, drone, nose, (255, 255, 255), 2, cv2.LINE_AA)

    max_risk = max(lidar_risk.values(), default=0.0)
    status = f"frame {frame_index}  photos {len(saved_ids)}/{len(targets)}  max lidar risk {max_risk:.2f}"
    cv2.putText(canvas, status, (24, 32), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (225, 232, 235), 2, cv2.LINE_AA)
    cv2.putText(canvas, "Town bounds / targets / FPV trail", (24, canvas_h - 24),
                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (180, 190, 195), 1, cv2.LINE_AA)
    return canvas


def parse_capture(text: str):
    parts = text[5:].split(":")
    target_id = parts[0].strip() if parts else ""
    classification = parts[1].strip() if len(parts) > 1 else None
    confidence = None
    if len(parts) > 2:
        try:
            confidence = float(parts[2])
        except ValueError:
            confidence = None
    return target_id, classification, confidence


def _carla_location(origin, dx: float, dy: float, dz: float, z_lift: float = 0.0):
    import carla  # noqa: WPS433

    return carla.Location(x=origin.x + dx, y=origin.y - dy, z=origin.z - dz + z_lift)


def _target_location(target, origin, z_lift: float = 1.2):
    import carla  # noqa: WPS433

    loc = target.get("worldLocation")
    if loc is not None:
        return carla.Location(x=loc.x, y=loc.y, z=loc.z + z_lift)
    actor = target.get("actor")
    if actor is not None:
        try:
            loc = actor.get_location()
            return carla.Location(x=loc.x, y=loc.y, z=loc.z + z_lift)
        except Exception:  # noqa: BLE001
            pass
    return carla.Location(x=origin.x + target["x"], y=origin.y - target["y"], z=origin.z + z_lift)


def _hit_distance(start, hit):
    return math.sqrt(
        (hit.location.x - start.x) ** 2
        + (hit.location.y - start.y) ** 2
        + (hit.location.z - start.z) ** 2
    )


def _has_line_of_sight_between(world, start, end, *, clearance: float = 3.0) -> bool:
    target_distance = max(1.0, start.distance(end))
    try:
        hits = world.cast_ray(start, end)
    except Exception:  # noqa: BLE001
        return True
    for hit in hits:
        label = str(hit.label)
        distance = _hit_distance(start, hit)
        if label in NON_BLOCKING_LABELS:
            continue
        if "Buildings" in label and distance < target_distance - clearance:
            return False
    return True


def has_line_of_sight(world, origin, drone_offset, target, *, clearance: float = 3.0) -> bool:
    start = _carla_location(origin, drone_offset[0], drone_offset[1], drone_offset[2], z_lift=-0.2)
    end = _target_location(target, origin)
    return _has_line_of_sight_between(world, start, end, clearance=clearance)


def has_camera_line_of_sight(world, camera_transform, target, *, clearance: float = 3.0) -> bool:
    if camera_transform is None:
        return True
    return _has_line_of_sight_between(
        world, camera_transform.location, _target_location(target, camera_transform.location),
        clearance=clearance)


def _first_blocking_hit_fraction(world, start, end):
    total = max(1.0, start.distance(end))
    try:
        hits = world.cast_ray(start, end)
    except Exception:  # noqa: BLE001
        return 1.0
    best = total
    for hit in hits:
        label = str(hit.label)
        if label in NON_BLOCKING_LABELS:
            continue
        best = min(best, _hit_distance(start, hit))
    return max(0.0, min(1.0, best / total))


def _action_vector(action_id: str, dx: float, dy: float, bounds: dict):
    if action_id == "return-to-area":
        cx = (bounds["minX"] + bounds["maxX"]) / 2.0 - dx
        cy = (bounds["minY"] + bounds["maxY"]) / 2.0 - dy
        length = max(0.001, math.hypot(cx, cy))
        return cx / length, cy / length
    return ACTION_VECTORS.get(action_id, (0.0, 0.0))


def _carla_action_vector(action_id: str, dx: float, dy: float, bounds: dict):
    """Return a normalized CARLA-frame action vector from an AirSim-frame action id."""
    ax, ay = _action_vector(action_id, dx, dy, bounds)
    return ax, -ay


def _corridor_blocking_fraction(world, start, vx: float, vy: float, lookahead: float,
                                radius: float, z_samples: tuple[float, ...]):
    import carla  # noqa: WPS433

    length = max(0.001, math.hypot(vx, vy))
    ux, uy = vx / length, vy / length
    px, py = -uy, ux
    best_fraction = 1.0
    for lateral in (-radius, 0.0, radius):
        for z_lift in z_samples:
            sample_start = carla.Location(
                x=start.x + px * lateral,
                y=start.y + py * lateral,
                z=start.z + z_lift,
            )
            sample_end = carla.Location(
                x=sample_start.x + ux * lookahead,
                y=sample_start.y + uy * lookahead,
                z=sample_start.z,
            )
            best_fraction = min(best_fraction, _first_blocking_hit_fraction(world, sample_start, sample_end))
    return best_fraction


def carla_lidar_risks(world, origin, drone_offset, bounds: dict, lookahead: float,
                      corridor_radius: float = 6.0):
    import carla  # noqa: WPS433

    risks = {}
    start = _carla_location(origin, drone_offset[0], drone_offset[1], drone_offset[2], z_lift=0.0)
    z_samples = (-4.0, 0.0, 4.0)
    for action_id in ACTION_VECTORS:
        vx, vy = _carla_action_vector(action_id, drone_offset[0], drone_offset[1], bounds)
        fraction = _corridor_blocking_fraction(world, start, vx, vy, lookahead, corridor_radius, z_samples)
        risks[action_id] = round(1.0 - fraction, 3)
    # Return-to-area is a real movement option in the worker, even though it is not a fixed vector.
    vx, vy = _carla_action_vector("return-to-area", drone_offset[0], drone_offset[1], bounds)
    risks["return-to-area"] = round(1.0 - _corridor_blocking_fraction(
        world, start, vx, vy, lookahead, corridor_radius, z_samples), 3)
    return risks


def airsim_lidar_summary(client, yaw_deg: float):
    try:
        data = client.getLidarData()
        points = list(data.point_cloud)
    except Exception:  # noqa: BLE001
        points = []
    if len(points) < 3:
        return {"available": False, "points": 0, "minDistanceMeters": None}
    yaw = math.radians(yaw_deg)
    bins = {key: [] for key in ("front", "left", "right", "rear")}
    distances = []
    for i in range(0, len(points) - 2, 3):
        x, y, z = points[i], points[i + 1], points[i + 2]
        distance = math.sqrt(x * x + y * y + z * z)
        if distance <= 0.2:
            continue
        distances.append(distance)
        wx = x * math.cos(yaw) - y * math.sin(yaw)
        wy = x * math.sin(yaw) + y * math.cos(yaw)
        angle = math.atan2(wy, wx)
        if -math.pi / 4 <= angle <= math.pi / 4:
            bins["front"].append(distance)
        elif math.pi / 4 < angle < 3 * math.pi / 4:
            bins["left"].append(distance)
        elif -3 * math.pi / 4 < angle < -math.pi / 4:
            bins["right"].append(distance)
        else:
            bins["rear"].append(distance)
    return {
        "available": True,
        "points": len(distances),
        "minDistanceMeters": round(min(distances), 3) if distances else None,
        "frontMinMeters": round(min(bins["front"]), 3) if bins["front"] else None,
        "leftMinMeters": round(min(bins["left"]), 3) if bins["left"] else None,
        "rightMinMeters": round(min(bins["right"]), 3) if bins["right"] else None,
        "rearMinMeters": round(min(bins["rear"]), 3) if bins["rear"] else None,
    }


# --------------------------------------------------------------------------- phase 1: pretrain

def run_pretrain(args) -> int:
    import airsim  # noqa: WPS433
    import carla  # noqa: WPS433
    import cv2
    import numpy as np

    client = _airsim_takeoff(args)
    origin = client.getMultirotorState().kinematics_estimated.position
    carla_client = carla.Client(args.carla_host, args.carla_port)
    carla_client.set_timeout(args.timeout)
    world = carla_client.get_world()
    world.set_weather(carla.WeatherParameters.ClearNoon)
    carla_origin = carla.Location(x=origin.x_val, y=origin.y_val, z=0.0)
    targets, _bounds = spawn_targets(
        world, carla_origin,
        args.area_half, not args.allow_waterfront_targets,
        exact_search_area=not args.fit_search_area_to_town)
    initial_transform = carla.Transform(
        carla.Location(x=carla_origin.x, y=carla_origin.y, z=max(2.0, args.altitude)),
        carla.Rotation(pitch=args.camera_pitch, yaw=0.0, roll=0.0),
    )
    carla_camera = CarlaRgbCamera(world, args.width, args.height, args.fov, args.fps, initial_transform) \
        if args.camera_source == "carla-rgb" else None

    cam = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    cam_dest = (args.java_host, args.camera_port)
    dataset_file = None
    evidence_dir = None
    if args.dataset_output:
        dataset_path = Path(args.dataset_output).resolve()
        dataset_path.parent.mkdir(parents=True, exist_ok=True)
        dataset_file = dataset_path.open("w", encoding="utf-8")
    if args.recognition_evidence_output:
        evidence_dir = Path(args.recognition_evidence_output).resolve()
        (evidence_dir / "source").mkdir(parents=True, exist_ok=True)
        (evidence_dir / "ground-truth").mkdir(parents=True, exist_ok=True)
        (evidence_dir / "crops").mkdir(parents=True, exist_ok=True)
    sent = 0
    try:
        for target in targets:
            per_target_sent = 0
            # For the nadir FPV training setup, hover directly above each target so the
            # labeled 3x3-patch crops contain the actor instead of nearby context.
            tx, ty = target["x"], target["y"]
            client.moveToPositionAsync(origin.x_val + tx, origin.y_val + ty,
                                       -args.pretrain_altitude, args.pretrain_speed).join()
            time.sleep(0.8)
            for shot in range(args.shots):
                state = client.getMultirotorState().kinematics_estimated
                pos = state.position
                _, _, yaw = airsim.to_eularian_angles(state.orientation)
                yaw_deg = math.degrees(yaw)
                dx, dy, dz = pos.x_val - origin.x_val, pos.y_val - origin.y_val, pos.z_val
                drone_offset = (dx, dy, dz)
                altitude_m = max(2.0, -pos.z_val)
                frame_transform = None
                if carla_camera is not None:
                    frame_transform = carla_camera_transform(
                        carla_origin, drone_offset, altitude_m, yaw_deg, args.camera_pitch, args.camera_yaw_offset)
                    bgr = carla_camera.read(frame_transform)
                    if bgr is None:
                        continue
                    camera_pose = None
                else:
                    response = client.simGetImages([airsim.ImageRequest("0", airsim.ImageType.Scene, False, False)])[0]
                    bgr = decode_airsim_scene_bgr(response, args.width, args.height)
                    try:
                        camera_pose = client.simGetCameraInfo("0").pose
                    except Exception:  # noqa: BLE001
                        camera_pose = None
                camera_yaw_deg = yaw_deg + args.camera_yaw_offset
                box = visible_context_box(target, origin, camera_pose, drone_offset, camera_yaw_deg,
                                          args.camera_pitch, args.width, args.height, args.fov, bgr,
                                          frame_transform)
                if box is None and args.pretrain_allow_projected_fallback:
                    projected = target_box(target, origin, camera_pose, (dx, dy, dz), camera_yaw_deg,
                                           args.camera_pitch, args.width, args.height, args.fov)
                    if usable_projected_box(projected, args.width, args.height):
                        box = projected
                if box is None:
                    continue
                accepted, reason, metrics = evaluate_target_evidence(target, bgr, box, args)
                if not accepted:
                    print(f"[pretrain] skipped {target['id']}: {reason}")
                    continue
                gray = crop_gray_pixels(bgr, box, args.downsample, args.crop_pad, args.crop_min_half)
                image_path = None
                ground_truth_path = None
                crop_path = None
                if evidence_dir is not None:
                    stem = f"{sent:04d}-{target['id']}-{target['label']}"
                    image_path = (evidence_dir / "source" / f"{stem}.png").resolve()
                    ground_truth_path = (evidence_dir / "ground-truth" / f"{stem}.png").resolve()
                    crop_path = (evidence_dir / "crops" / f"{stem}.png").resolve()
                    cv2.imwrite(str(image_path), bgr)
                    annotated = annotate_target(bgr.copy(), target, box, " gt")
                    cv2.imwrite(str(ground_truth_path), annotated)
                    crop_box = expanded_box(box, args.width, args.height, args.crop_pad, args.crop_min_half)
                    if crop_box is not None:
                        x1, y1, x2, y2 = crop_box
                        cv2.imwrite(str(crop_path), bgr[y1:y2, x1:x2])
                row = {
                    "frame": sent, "missionId": "pretrain", "uavId": "uav-1",
                    "frameId": f"pretrain-{target['id']}-{sent}",
                    "trackId": target["id"], "label": target["label"],
                    "detectionBox": {"cx": box[0], "cy": box[1], "w": box[2], "h": box[3]} if box else None,
                    "evidenceMetrics": metrics,
                    "pixels": gray.astype(int).tolist(),
                }
                if image_path is not None:
                    row["imagePath"] = str(image_path)
                    row["groundTruthImagePath"] = str(ground_truth_path)
                    if crop_path is not None and crop_path.exists():
                        row["cropImagePath"] = str(crop_path)
                if dataset_file is not None:
                    dataset_file.write(json.dumps(row) + "\n")
                if not args.dataset_only:
                    cam.sendto(json.dumps(row).encode("utf-8"), cam_dest)
                sent += 1
                per_target_sent += 1
                time.sleep(0.15)
            print(f"[pretrain] streamed {per_target_sent} labeled crops for {target['id']} ({target['label']})")
    finally:
        if dataset_file is not None:
            dataset_file.close()
        cam.close()
        if carla_camera is not None:
            carla_camera.destroy()
        _airsim_land(client)
        for target in targets:
            if target["actor"] is not None:
                try:
                    target["actor"].destroy()
                except Exception:  # noqa: BLE001
                    pass
    print(f"[pretrain] done; {sent} labeled FPV crops streamed to the Java recognition trainer.")
    return 0


# --------------------------------------------------------------------------- phase 2/3: loop

def run_bridge(args) -> int:
    import airsim  # noqa: WPS433
    import carla  # noqa: WPS433
    import cv2
    import numpy as np

    output = Path(args.output).resolve()
    photos = output / "photos"
    photos.mkdir(parents=True, exist_ok=True)

    client = _airsim_takeoff(args)
    origin = client.getMultirotorState().kinematics_estimated.position
    carla_client = carla.Client(args.carla_host, args.carla_port)
    carla_client.set_timeout(args.timeout)
    world = carla_client.get_world()
    world.set_weather(carla.WeatherParameters.ClearNoon)
    carla_origin = carla.Location(x=origin.x_val, y=origin.y_val, z=0.0)
    targets, bounds = spawn_targets(
        world, carla_origin,
        args.area_half, not args.allow_waterfront_targets)
    targets_by_id = {target["id"]: target for target in targets}
    if args.start_at_town_center and not args.allow_waterfront_targets:
        start_x, start_y = town_center_start()
    else:
        start_x = (bounds["minX"] + bounds["maxX"]) / 2.0
        start_y = (bounds["minY"] + bounds["maxY"]) / 2.0
    client.moveToPositionAsync(origin.x_val + start_x, origin.y_val + start_y,
                               -args.altitude, max(4.0, args.speed)).join()
    if args.initial_yaw is not None:
        try:
            client.rotateToYawAsync(args.initial_yaw).join()
        except Exception:  # noqa: BLE001
            pass
    time.sleep(0.4)

    initial_transform = carla.Transform(
        carla.Location(x=carla_origin.x + start_x, y=carla_origin.y - start_y, z=max(2.0, args.altitude)),
        carla.Rotation(pitch=args.camera_pitch, yaw=0.0, roll=0.0),
    )
    carla_camera = CarlaRgbCamera(world, args.width, args.height, args.fov, args.fps, initial_transform) \
        if args.camera_source == "carla-rgb" else None

    mav = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    mav.settimeout(0.0)
    mav.bind(("0.0.0.0", 0))
    mav_dest = (args.java_host, args.mavlink_port)
    cam = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    cam_dest = (args.java_host, args.camera_port)

    video = None
    topdown_video = None
    if args.record:
        codec = cv2.VideoWriter_fourcc(*"mp4v")
        video = cv2.VideoWriter(str(output / "fpv-camera.mp4"), codec, args.fps, (args.width, args.height))
        topdown_video = cv2.VideoWriter(str(output / "topdown-record.mp4"), codec, args.fps, (960, 720))

    photographed: dict[str, float] = {}  # target_id -> bbox TTL expiry (epoch seconds)
    saved_ids: set[str] = set()
    last_candidate_evidence = {}
    evidence_reject_counts = {}
    trail: list[tuple[float, float]] = []
    lidar_risk_cache = {action_id: 0.0 for action_id in ACTION_VECTORS}
    lidar_risk_cache["return-to-area"] = 0.0
    lidar_cache_frame = -999
    seq = 0
    frame_index = 0
    movement_commands = 0
    start = time.time()
    width = bounds["maxX"] - bounds["minX"]
    height = bounds["maxY"] - bounds["minY"]
    placement = "inland town" if not args.allow_waterfront_targets else "full town"
    print(f"[bridge] {placement} area {width:.0f}x{height:.0f} m, 4 vehicles + 4 humans; "
          f"streaming to worker; record={args.record}")
    try:
        while time.time() - start < args.duration:
            frame_index += 1
            now = time.time()
            state = client.getMultirotorState().kinematics_estimated
            pos = state.position
            dx, dy, dz = pos.x_val - origin.x_val, pos.y_val - origin.y_val, pos.z_val
            _, _, yaw = airsim.to_eularian_angles(state.orientation)
            yaw_deg = math.degrees(yaw)
            drone_offset = (dx, dy, dz)
            altitude_m = max(2.0, -pos.z_val)
            camera_footprint = estimate_camera_footprint(altitude_m, args.fov, args.camera_pitch)
            camera_yaw_deg = yaw_deg + args.camera_yaw_offset
            frame_transform = None
            trail.append((dx, dy))
            if len(trail) > 2400:
                trail.pop(0)

            lat, lon = local_to_global(dx, dy)
            try:
                mav.sendto(encode_global_position_int(
                    seq, 1, 1, time_boot_ms=frame_index * 100, lat=lat, lon=lon,
                    alt=int(-dz * 1000), rel_alt=int(-dz * 1000),
                    vx=int(state.linear_velocity.x_val * 100), vy=int(state.linear_velocity.y_val * 100),
                    vz=int(state.linear_velocity.z_val * 100), hdg=int(yaw_deg * 100) % 36000), mav_dest)
            except OSError:
                pass
            seq = (seq + 1) % 256

            if carla_camera is not None:
                frame_transform = carla_camera_transform(
                    carla_origin, drone_offset, altitude_m, yaw_deg, args.camera_pitch, args.camera_yaw_offset)
                bgr = carla_camera.read(frame_transform)
                if bgr is None:
                    continue
                camera_pose = None
            else:
                response = client.simGetImages([airsim.ImageRequest("0", airsim.ImageType.Scene, False, False)])[0]
                bgr = decode_airsim_scene_bgr(response, args.width, args.height)
                try:
                    camera_pose = client.simGetCameraInfo("0").pose
                except Exception:  # noqa: BLE001
                    camera_pose = None
            lidar = airsim_lidar_summary(client, yaw_deg)
            if frame_index - lidar_cache_frame >= max(1, args.lidar_skip):
                lidar_lookahead = max(
                    args.lidar_range,
                    args.speed * args.velocity_scale * args.command_seconds * args.lidar_time_horizon,
                )
                lidar_risk_cache = carla_lidar_risks(
                    world, carla_origin,
                    drone_offset, bounds, lidar_lookahead, args.lidar_corridor_radius)
                lidar_cache_frame = frame_index
            lidar_risk = lidar_risk_cache

            remaining = [t for t in targets if t["id"] not in saved_ids]
            nearest = min(remaining, key=lambda t: math.hypot(t["x"] - dx, t["y"] - dy), default=None)
            occluded = None
            candidate_distance, candidate, candidate_box = None, None, None
            projected = []
            for target in remaining:
                box = visible_context_box(target, origin, camera_pose, (dx, dy, dz), camera_yaw_deg,
                                          args.camera_pitch, args.width, args.height, args.fov, bgr,
                                          frame_transform)
                if box is None:
                    continue
                accepted, reason, metrics = evaluate_target_evidence(target, bgr, box, args)
                if not accepted:
                    evidence_reject_counts[reason] = evidence_reject_counts.get(reason, 0) + 1
                    continue
                projected.append((math.hypot(target["x"] - dx, target["y"] - dy), target, box, metrics))
            for distance, target, box, metrics in sorted(projected, key=lambda item: item[0]):
                los_ok = has_camera_line_of_sight(world, frame_transform, target) if frame_transform is not None \
                    else has_line_of_sight(world, carla_origin, drone_offset, target)
                if not los_ok:
                    if occluded is None:
                        occluded = (distance, target)
                    continue
                candidate_distance, candidate, candidate_box = distance, target, box
                break
            gray = crop_gray_pixels(bgr, candidate_box, args.downsample, args.crop_pad, args.crop_min_half)
            observation = {
                "frame": frame_index, "elapsedSeconds": round(now - start, 3),
                "missionId": "carla-air-live", "uavId": "uav-1",
                "positionOffset": {"x": round(dx, 3), "y": round(dy, 3)},
                "searchBounds": search_bounds(bounds),
                "photographedTargets": len(saved_ids), "remainingTargets": len(remaining),
                "photographedTargetIds": sorted(saved_ids),
                "totalTargets": len(targets), "baseSpeedMetersPerSecond": args.speed,
                "photoRadiusMeters": PHOTO_RADIUS, "cameraFootprintMeters": round(camera_footprint, 3),
                "headingYawDegrees": round(camera_yaw_deg, 3),
                "trackId": candidate["id"] if candidate else None,
                "obstacleRiskByAction": lidar_risk,
                "lidarRiskByAction": lidar_risk,
                "lidar": lidar,
                "pixels": gray.astype(int).tolist(),
            }
            if nearest and args.expose_hidden_nearest_target:
                observation["nearestTarget"] = {
                    "vector": {"x": round(nearest["x"] - dx, 3), "y": round(nearest["y"] - dy, 3)},
                    "distanceMeters": round(math.hypot(nearest["x"] - dx, nearest["y"] - dy), 3),
                }
            if candidate:
                last_candidate_evidence[candidate["id"]] = (now, candidate_box, bgr.copy(), metrics)
                observation["candidateTarget"] = {
                    "id": candidate["id"],
                    "distanceMeters": round(candidate_distance, 3),
                }
                observation["label"] = candidate["label"]
                observation["detectionBox"] = {
                    "cx": candidate_box[0], "cy": candidate_box[1],
                    "w": candidate_box[2], "h": candidate_box[3],
                }
            if occluded:
                _, blocked = occluded
                observation["occludedTarget"] = {
                    "id": blocked["id"],
                    "vector": {"x": round(blocked["x"] - dx, 3), "y": round(blocked["y"] - dy, 3)},
                    "distanceMeters": round(math.hypot(blocked["x"] - dx, blocked["y"] - dy), 3),
                }
            try:
                cam.sendto(json.dumps(observation).encode("utf-8"), cam_dest)
            except OSError:
                pass

            pending_move = None
            while True:
                try:
                    data, _ = mav.recvfrom(4096)
                except (BlockingIOError, socket.timeout):
                    break
                except ConnectionResetError:
                    # Windows surfaces a prior ICMP "port unreachable" here; benign for UDP.
                    break
                parsed = decode_statustext(data)
                if not parsed:
                    continue
                text = parsed[1]
                if text.startswith("JM:"):
                    pending_move = tuple(float(v) for v in text[3:].split(",")[:4])
                elif text.startswith("JCAP:"):
                    target_id, classification, confidence = parse_capture(text)
                    target = targets_by_id.get(target_id)
                    if target is None:
                        print(f"[bridge] ignored capture for unknown target id {target_id!r}")
                        continue
                    if target_id in saved_ids:
                        continue
                    if confidence is None or confidence <= args.photo_confidence_threshold:
                        shown = "none" if confidence is None else f"{confidence:.2f}"
                        print(f"[bridge] rejected {target_id}: confidence {shown} not over "
                              f"{args.photo_confidence_threshold:.2f}")
                        continue
                    expected_class = java_class(target["label"])
                    if classification and classification != expected_class:
                        print(f"[bridge] rejected {target_id}: predicted {classification}, expected {expected_class}")
                        continue
                    photo_frame = bgr
                    box = visible_context_box(target, origin, camera_pose, (dx, dy, dz), camera_yaw_deg,
                                              args.camera_pitch, args.width, args.height, args.fov, bgr,
                                              frame_transform)
                    evidence_ok = False
                    evidence_reason = "NO_EVALUABLE_BOX"
                    if box is None:
                        cached = last_candidate_evidence.get(target_id)
                        if cached is not None and now - cached[0] <= args.capture_evidence_ttl:
                            box = cached[1]
                            photo_frame = cached[2]
                            evidence_ok = True
                            evidence_reason = "TARGET_EVIDENCE_OK_CACHED"
                    else:
                        evidence_ok, evidence_reason, _metrics = evaluate_target_evidence(target, bgr, box, args)
                        if not evidence_ok:
                            cached = last_candidate_evidence.get(target_id)
                            if cached is not None and now - cached[0] <= args.capture_evidence_ttl:
                                box = cached[1]
                                photo_frame = cached[2]
                                evidence_ok = True
                                evidence_reason = "TARGET_EVIDENCE_OK_CACHED"
                    if box is None:
                        print(f"[bridge] rejected {target_id}: no visible bounding box at capture time")
                        continue
                    if not evidence_ok:
                        evidence_reject_counts[evidence_reason] = evidence_reject_counts.get(evidence_reason, 0) + 1
                        print(f"[bridge] rejected {target_id}: {evidence_reason}")
                        continue
                    los_ok = has_camera_line_of_sight(world, frame_transform, target) if frame_transform is not None \
                        else has_line_of_sight(world, carla_origin, drone_offset, target)
                    if not los_ok:
                        print(f"[bridge] rejected {target_id}: line of sight blocked")
                        continue
                    photographed[target_id] = now + args.box_ttl
                    saved_ids.add(target_id)
                    annotated_photo = photo_frame.copy()
                    suffix = "" if confidence is None else f" {confidence:.2f}"
                    annotate_target(annotated_photo, target, box, suffix)
                    cv2.imwrite(str(photos / f"{target_id}-frame{frame_index}.png"), annotated_photo)
                    print(f"[bridge] saved recognized-target photo for {target_id}")

            if pending_move is not None:
                vx, vy, alt, cmd_yaw = pending_move
                try:
                    scaled_vx = vx * args.velocity_scale
                    scaled_vy = vy * args.velocity_scale
                    if args.position_actuator:
                        current = client.getMultirotorState().kinematics_estimated.position
                        movement = client.moveToPositionAsync(
                            current.x_val + scaled_vx * args.command_seconds,
                            current.y_val + scaled_vy * args.command_seconds,
                            -alt,
                            max(1.0, math.hypot(scaled_vx, scaled_vy)),
                            drivetrain=airsim.DrivetrainType.MaxDegreeOfFreedom,
                            yaw_mode=airsim.YawMode(False, cmd_yaw))
                    else:
                        movement = client.moveByVelocityZAsync(
                            scaled_vx, scaled_vy, -alt, args.command_seconds,
                            drivetrain=airsim.DrivetrainType.MaxDegreeOfFreedom,
                            yaw_mode=airsim.YawMode(False, cmd_yaw))
                    if not args.async_movement_command:
                        movement.join()
                    movement_commands += 1
                except Exception as exc:  # noqa: BLE001
                    print(f"[bridge] movement command failed: {exc}")

            if video is not None:
                annotated = bgr.copy()
                for target in targets:
                    expiry = photographed.get(target["id"])
                    if expiry is None or now > expiry:  # 2 s TTL on each target's box
                        continue
                    box = visible_context_box(target, origin, camera_pose, (dx, dy, dz), camera_yaw_deg,
                                              args.camera_pitch, args.width, args.height, args.fov, annotated,
                                              frame_transform)
                    los_ok = has_camera_line_of_sight(world, frame_transform, target) if frame_transform is not None \
                        else has_line_of_sight(world, carla_origin, drone_offset, target)
                    if box is None or not los_ok:
                        continue
                    annotate_target(annotated, target, box)
                video.write(annotated)
            if topdown_video is not None:
                topdown_video.write(render_topdown_frame(
                    bounds, targets, drone_offset, yaw_deg, trail, saved_ids,
                    photographed, now, lidar_risk, frame_index))

            time.sleep(max(0.0, frame_index / args.fps - (time.time() - start)))
    finally:
        if video is not None:
            video.release()
        if topdown_video is not None:
            topdown_video.release()
        if carla_camera is not None:
            carla_camera.destroy()
        _airsim_land(client)
        mav.close()
        cam.close()
        for target in targets:
            if target["actor"] is not None:
                try:
                    target["actor"].destroy()
                except Exception:  # noqa: BLE001
                    pass
    video_status = "fpv-camera.mp4 + topdown-record.mp4" if args.record else "off"
    x_span = y_span = 0.0
    if trail:
        xs = [item[0] for item in trail]
        ys = [item[1] for item in trail]
        x_span = max(xs) - min(xs)
        y_span = max(ys) - min(ys)
        print(f"[bridge] movement evidence: commands={movement_commands}; "
              f"xSpan={x_span:.1f}m; ySpan={y_span:.1f}m")
    summary = {
        "mode": "carla-air-jneopallium-bridge",
        "bounds": bounds,
        "areaMeters": {"width": bounds["maxX"] - bounds["minX"], "height": bounds["maxY"] - bounds["minY"]},
        "movementCommandsApplied": movement_commands,
        "movementSpanMeters": {"x": round(x_span, 3), "y": round(y_span, 3)},
        "targetsPhotographed": sorted(saved_ids),
        "targetCount": len(targets),
        "targetEvidenceRejectCounts": evidence_reject_counts,
        "photoConfidenceThresholdStrictlyGreaterThan": args.photo_confidence_threshold,
        "hiddenNearestTargetExposed": args.expose_hidden_nearest_target,
        "movementCommandMode": "async" if args.async_movement_command else "joined",
        "actuator": "position" if args.position_actuator else "velocity",
        "velocityScale": args.velocity_scale,
        "video": video_status,
        "photosDir": str(photos),
    }
    (output / "bridge-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(f"[bridge] done. photographed {sorted(saved_ids)} / {len(targets)}; "
          f"video={video_status}; photos={photos}")
    return 0


# --------------------------------------------------------------------------- airsim helpers

def _airsim_takeoff(args):
    import airsim  # noqa: WPS433

    client = airsim.MultirotorClient()
    client.confirmConnection()
    client.reset()          # return the drone to PlayerStart (over the city) each run
    time.sleep(1.0)
    client.enableApiControl(True)
    client.armDisarm(True)
    client.takeoffAsync().join()
    client.moveToZAsync(-args.altitude, 4).join()
    try:
        client.simSetCameraFov("0", args.fov)
        client.simSetCameraPose(
            "0",
            airsim.Pose(
                airsim.Vector3r(0.0, 0.0, 0.0),
                airsim.to_quaternion(math.radians(args.camera_pitch), 0.0,
                                     math.radians(args.camera_yaw_offset)),
            ),
        )
    except Exception:  # noqa: BLE001
        pass
    return client


def _airsim_land(client) -> None:
    try:
        client.hoverAsync().join()
        client.landAsync().join()
        client.armDisarm(False)
        client.enableApiControl(False)
    except Exception:  # noqa: BLE001
        pass


# --------------------------------------------------------------------------- mavlink self-test

def mavlink_selftest(args) -> int:
    mav = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    mav.settimeout(0.1)
    cam = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    mav_dest = (args.java_host, args.mavlink_port)
    cam_dest = (args.java_host, args.camera_port)
    print(f"[selftest] telemetry->{mav_dest}, camera->{cam_dest}; listening for worker commands...")
    received = 0
    seq = 0
    gray = [[120 for _ in range(8)] for _ in range(8)]
    deadline = time.time() + args.duration
    while time.time() < deadline:
        x, y = 10.0 * math.sin(seq / 10.0), 5.0 * math.cos(seq / 10.0)
        lat, lon = local_to_global(x, y)
        mav.sendto(encode_global_position_int(seq, 1, 1, time_boot_ms=seq * 100,
                                              lat=lat, lon=lon, alt=38000, rel_alt=38000,
                                              vx=0, vy=0, vz=0, hdg=0), mav_dest)
        cam.sendto(json.dumps({
            "frame": seq, "missionId": "selftest", "uavId": "uav-1",
            "positionOffset": {"x": x, "y": y}, "searchBounds": search_bounds(),
            "nearestTarget": {"vector": {"x": 90.0 - x, "y": 60.0 - y}, "distanceMeters": math.hypot(90.0 - x, 60.0 - y)},
            "photographedTargets": 0, "remainingTargets": 8, "totalTargets": 8,
            "baseSpeedMetersPerSecond": 10.0, "photoRadiusMeters": PHOTO_RADIUS, "headingYawDegrees": 0.0,
            "pixels": gray,
        }).encode("utf-8"), cam_dest)
        seq = (seq + 1) % 256
        try:
            data, _ = mav.recvfrom(4096)
        except (BlockingIOError, socket.timeout):
            continue
        parsed = decode_statustext(data)
        if parsed:
            received += 1
            print(f"[selftest] worker command: severity={parsed[0]} text={parsed[1]!r}")
    mav.close()
    cam.close()
    print(f"[selftest] commands received from worker: {received} -> {'PASS' if received else 'NO COMMANDS'}")
    return 0 if received else 2


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Thin CARLA-Air <-> jneopallium MAVLink/camera bridge.")
    parser.add_argument("--mode", choices=("bridge", "pretrain", "mavlink-selftest"), default="bridge")
    parser.add_argument("--java-host", default="127.0.0.1")
    parser.add_argument("--mavlink-port", type=int, default=14550)
    parser.add_argument("--camera-port", type=int, default=14552)
    parser.add_argument("--carla-host", default="127.0.0.1")
    parser.add_argument("--carla-port", type=int, default=2000)
    parser.add_argument("--timeout", type=float, default=20.0)
    parser.add_argument("--output", default="target/carla-air-live")
    parser.add_argument("--duration", type=float, default=300.0)
    parser.add_argument("--record", action="store_true")
    parser.add_argument("--fps", type=int, default=12)
    parser.add_argument("--width", type=int, default=1920)
    parser.add_argument("--height", type=int, default=1080)
    parser.add_argument("--downsample", type=int, default=64)
    parser.add_argument("--crop-pad", type=float, default=2.4,
                        help="Recognition crop scale around the projected target box.")
    parser.add_argument("--crop-min-half", type=int, default=18,
                        help="Minimum half-size in pixels for recognition crops.")
    parser.add_argument("--disable-strict-target-evidence", dest="strict_target_evidence",
                        action="store_false",
                        help="Disable hidden rendered-target evidence checks; useful only for debugging.")
    parser.set_defaults(strict_target_evidence=True)
    parser.add_argument("--min-target-edge-ratio", type=float, default=0.018)
    parser.add_argument("--min-target-center-edge-ratio", type=float, default=0.010)
    parser.add_argument("--min-target-foreground-ratio", type=float, default=0.055)
    parser.add_argument("--min-target-center-foreground-ratio", type=float, default=0.045)
    parser.add_argument("--min-target-texture", type=float, default=22.0)
    parser.add_argument("--max-target-center-water-ratio", type=float, default=0.18)
    parser.add_argument("--max-frame-water-ratio", type=float, default=0.35)
    parser.add_argument("--min-vehicle-box-aspect", type=float, default=0.90)
    parser.add_argument("--max-walker-box-aspect", type=float, default=1.10)
    parser.add_argument("--altitude", type=float, default=60.0)
    parser.add_argument("--speed", type=float, default=12.0)
    parser.add_argument("--command-seconds", type=float, default=0.6)
    parser.add_argument("--velocity-scale", type=float, default=1.0,
                        help="Actuator calibration multiplier for CARLA-Air horizontal velocity response.")
    parser.add_argument("--position-actuator", action="store_true",
                        help="Execute JNeoPallium velocity commands as short move-to-position setpoints.")
    parser.add_argument("--async-movement-command", action="store_true",
                        help="Do not wait for each AirSim velocity command interval before the next camera tick.")
    parser.add_argument("--camera-pitch", type=float, default=-75.0)
    parser.add_argument("--camera-yaw-offset", type=float, default=45.0,
                        help="Yaw offset of the mounted FPV camera relative to the vehicle body.")
    parser.add_argument("--fov", type=float, default=90.0)
    parser.add_argument("--camera-source", choices=("carla-rgb", "airsim"), default="carla-rgb",
                        help="FPV source. carla-rgb follows the drone with a CARLA RGB sensor so spawned actors render.")
    parser.add_argument("--initial-yaw", type=float, default=0.0,
                        help="Initial AirSim yaw before Jneopallium movement commands take over.")
    parser.add_argument("--box-ttl", type=float, default=2.0)
    parser.add_argument("--capture-evidence-ttl", type=float, default=8.0,
                        help="Seconds to retain a verified target frame for a delayed JCAP photo command.")
    parser.add_argument("--area-half", type=float, default=DEFAULT_AREA_HALF)
    parser.add_argument("--fit-search-area-to-town", action="store_true",
                        help="Use CARLA town spawn extents instead of the exact --area-half square.")
    parser.add_argument("--photo-confidence-threshold", type=float, default=0.70,
                        help="Bridge-side safety gate: save photos only when confidence is strictly over this value.")
    parser.add_argument("--expose-hidden-nearest-target", action="store_true",
                        help="Debug only: expose simulator ground-truth nearest target to movement.")
    parser.add_argument("--allow-waterfront-targets", action="store_true",
                        help="Use the full coastal Town10HD road graph instead of the inland-only target filter.")
    parser.add_argument("--start-at-town-center", action="store_true", default=True,
                        help="Start the scenario over the configured town block while keeping the requested search bounds.")
    parser.add_argument("--start-at-search-center", dest="start_at_town_center", action="store_false",
                        help="Start at the exact search-bound center instead of the configured town block.")
    parser.add_argument("--lidar-range", type=float, default=55.0)
    parser.add_argument("--lidar-time-horizon", type=float, default=1.8,
                        help="Minimum seconds of scaled movement covered by CARLA corridor ray-casts.")
    parser.add_argument("--lidar-corridor-radius", type=float, default=7.5,
                        help="Half-width in meters of the obstacle corridor cast for each action.")
    parser.add_argument("--lidar-skip", type=int, default=6)
    parser.add_argument("--shots", type=int, default=12)
    parser.add_argument("--pretrain-altitude", type=float, default=18.0)
    parser.add_argument("--pretrain-speed", type=float, default=8.0)
    parser.add_argument("--pretrain-allow-projected-fallback", action="store_true",
                        help="Training only: use the projected target box when town-context filtering is too strict.")
    parser.add_argument("--dataset-output",
                        help="Optional JSONL path for accepted labeled FPV crops captured during pretrain.")
    parser.add_argument("--dataset-only", action="store_true",
                        help="In pretrain mode, write the dataset but do not stream crops over UDP.")
    parser.add_argument("--recognition-evidence-output",
                        help="Optional folder for source, crop, and ground-truth annotated pretrain images.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.mode == "mavlink-selftest":
        return mavlink_selftest(args)
    if args.mode == "pretrain":
        return run_pretrain(args)
    return run_bridge(args)


if __name__ == "__main__":
    raise SystemExit(main())
