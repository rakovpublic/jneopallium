# JNeoBattlespace

JNeoBattlespace is the external simulation layer for FPV and swarm mission
acceptance. It has two backends:

- `IN_MEMORY`: deterministic, no external simulator dependencies, suitable for
  unit tests and ordinary CI.
- `JNEO_BATTLESPACE`: Linux live backend gated by dependency checks for Gazebo
  Harmonic, ArduPilot SITL, the official ArduPilot Gazebo plugin, ROS 2,
  `ros_gz_bridge`, and `rosbridge_suite`.

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
