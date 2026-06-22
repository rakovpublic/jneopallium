# JNeoBattlespace

JNeoBattlespace is the external simulation layer for FPV and swarm mission
acceptance. It has three backends:

- `IN_MEMORY`: deterministic, no external simulator dependencies, suitable for
  unit tests and ordinary CI.
- `JNEO_BATTLESPACE`: Linux live backend gated by dependency checks for Gazebo
  Harmonic, ArduPilot SITL, the official ArduPilot Gazebo plugin, ROS 2,
  `ros_gz_bridge`, and `rosbridge_suite`.
- `CARLA_AIR`: CARLA-Air/Unreal-oriented backend for urban FPV search. When a
  live CARLA-Air runtime is not installed, it writes deterministic CARLA-Air
  surrogate artifacts instead of pretending to launch Unreal.

The same scenario IDs, artifact contract, mission-supervisor checks, ground
truth firewall, radio routing model, perception adapter interface, and photo
evaluator are used by both backends. The live backend refuses to report success
when required external dependencies are missing.

## Dependency Check

```bash
simulators/jneo-battlespace/scripts/check_dependencies.sh
```

The script prints detected versions, gives installation guidance for missing
pieces, never runs `sudo`, and returns nonzero when the live backend cannot run.

## Deterministic Backend

```bash
python simulators/jneo-battlespace/supervisor/process_supervisor.py all --backend IN_MEMORY --headless
```

Artifacts are written under:

```text
target/jneo-battlespace/in_memory/<scenario>/
```

The `live_single_infantry_vehicle` scenario is included in `all`; it feeds
separate infantry and vehicle camera frames through the perception adapter,
photo validator, and virtual-elimination artifact path.

The `live_single_large_area_search` scenario performs a broad serpentine search,
records simulator bridge intents and top-down pose events, validates infantry and
vehicle photos through the same evaluator path, and emits `camera-video.*` plus
`top-down-video.*` from the run artifacts. Install optional recording
dependencies with `python -m pip install -r simulators/jneo-battlespace/requirements.txt`.

## CARLA-Air Backend

```bash
python simulators/jneo-battlespace/supervisor/process_supervisor.py carla_air_urban_search --backend CARLA_AIR --headless
```

The `carla_air_urban_search` scenario exercises the CARLA-Air integration
contract: Unreal-style FPV camera frames, dynamic vehicles, pedestrians, weather
and lighting, urban geometry and collision summaries, drone physics ticks,
JNeoBattlespace target lifecycle/scoring, radio constraints, and Jneopallium
camera perception, sensor fusion, target priority, autonomous navigation, and
swarm coordination decisions.

Use `--require-live-carla` when the run must fail unless the CARLA Python API
and CARLA-Air/Unreal runtime are installed. Without that flag, the backend still
runs the bridge/evaluator/controller stack and records deterministic surrogate
CARLA-Air artifacts for local development.

For this workspace, the Windows CARLA-Air release is expected under:

```text
.codex-tools/carla-air/CarlaAir-v0.1.7-Windows11-x86_64/
```

Start the live Unreal/AirSim runtime with:

```powershell
cd .codex-tools/carla-air/CarlaAir-v0.1.7-Windows11-x86_64
.\StartCarlaAir.bat Town10HD --res 1280x720 --quality Epic --traffic-vehicles 12 --traffic-walkers 20
```

## Autonomous FPV search + movement (jneopallium model in the loop)

The autonomous search-and-photograph behaviour lives in the jneopallium `uav-single-fpv`
model (recognition + the movement-policy head). The Java worker is the brain; the Python
script `supervisor/carla_air_jneopallium_bridge.py` is *only* an I/O bridge — telemetry and
motor commands on MAVLink UDP, FPV frames + perception on a camera UDP channel:

```text
CARLA-Air/AirSim --MAVLink+camera--> jneopallium worker (IInitInput -> recognition + movement
                                     -> IOutputAggregator) --MAVLink--> CARLA-Air/AirSim
```

Start the Java worker first (it binds the UDP ports and waits), then the bridge:

```powershell
# 1. jneopallium worker (movement RL + recognition, live wire)
java -cp "worker/target/classes;<deps>" `
  com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement.UavFpvAutonomyRun `
  --mode live --output target/uav-fpv-autonomy --duration 180

# 2. thin bridge against the running CarlaAir + AirSim multirotor (records FPV MP4 + photos)
.codex-tools/envs/carlaAir/python.exe `
  simulators/jneo-battlespace/supervisor/carla_air_jneopallium_bridge.py `
  --java-host 127.0.0.1 --output target/carla-air-live
```

A deterministic, CARLA-free verification of the same model + framework boundaries (produces
recognized-target photos + the decision/command stream over a loopback MAVLink transport):

```powershell
java -cp "worker/target/classes;<deps>" `
  com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement.UavFpvAutonomyRun `
  --mode selftest --ticks 600 --output target/uav-fpv-autonomy
```

Verify just the Python<->Java MAVLink/camera path (no CARLA) with the bridge's self-test:
`carla_air_jneopallium_bridge.py --mode mavlink-selftest`.

## Live Backend Gate

```bash
simulators/jneo-battlespace/scripts/run_scenario.sh all --backend JNEO_BATTLESPACE --headless
```

When dependencies are absent, each live scenario writes `SKIPPED_DEPENDENCY`
artifacts and the command returns nonzero. If dependencies are present, live
process startup still requires `--allow-live-processes` so accidental desktop or
CI runs do not spawn Gazebo, SITL, ROS, or Java child processes.

## Artifacts

Each scenario run emits the public files required by the integration guide:

- `manifest.json`
- `versions.json`
- `process-manifest.json`
- `vehicle-map.json`
- `scenario-config.json`
- `sensor-topic-health.json`
- `per-uav-camera-events.jsonl`
- `top-down-events.jsonl`
- `carla-air-events.jsonl`
- `sensor-fusion-events.jsonl`
- `jneopallium-decisions.jsonl`
- `mavlink-events.jsonl`
- `communication-events.jsonl`
- `flight-intents.jsonl`
- `command-audit.jsonl`
- `photograph-submissions.jsonl`
- `photograph-results.jsonl`
- `virtual-eliminations.jsonl`
- `score.json`
- `safety-summary.json`
- `ground-truth-firewall-report.json`
- `summary.json`
- `camera-video.gif` / `camera-video.mp4` for scenarios that enable recording
- `top-down-video.gif` / `top-down-video.mp4` for scenarios that enable recording

Evaluator-only evidence is written under `protected-evaluator/` and is not
mirrored into agent-facing ROS, MAVLink, or swarm-message artifacts.
