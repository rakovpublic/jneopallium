from __future__ import annotations

import argparse
import json
import platform
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPO_ROOT = ROOT.parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from supervisor.artifact_collector import ArtifactCollector
from supervisor.ground_truth_firewall import GroundTruthFirewall, default_agent_mappings
from supervisor.health_monitor import DependencyChecker
from supervisor.mission_supervisor import Backend
from supervisor.scenario_controller import ScenarioController, definition_for, scenario_ids
from supervisor.simulation_clock import SimulationClock
from supervisor.vehicle_map import generate_vehicle_map


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    backend = normalize_backend(args.backend)
    selected = scenario_ids() if args.scenario == "all" else [args.scenario]
    output_root = args.output or (REPO_ROOT / "target" / "jneo-battlespace")
    output_root = output_root.resolve()

    if backend == Backend.JNEO_BATTLESPACE:
        check = DependencyChecker().check(preview_enabled=args.preview)
        if not check["ok"]:
            summaries = [
                write_skipped_dependency_artifacts(
                    output_root=output_root,
                    backend=backend,
                    scenario_id=scenario_id,
                    dependency_check=check,
                    headless=args.headless,
                    seed=args.seed,
                )
                for scenario_id in selected
            ]
            write_suite_summary(output_root, backend, summaries)
            print(json.dumps({"status": "SKIPPED_DEPENDENCY", "missing": check["missing"]}, indent=2))
            return 2
        if not args.allow_live_processes:
            summaries = [
                write_live_not_started_artifacts(output_root, backend, scenario_id, check, args.headless, args.seed)
                for scenario_id in selected
            ]
            write_suite_summary(output_root, backend, summaries)
            print(
                "Live dependencies are present, but --allow-live-processes was not set; "
                "no Gazebo/SITL children were started.",
                file=sys.stderr,
            )
            return 3

    summaries: list[dict] = []
    for scenario_id in selected:
        definition = definition_for(scenario_id)
        run_dir = output_root / backend.value.lower() / scenario_id
        collector = ArtifactCollector(run_dir)
        vehicle_map = generate_vehicle_map(definition.vehicleCount, run_dir)
        collector.write_json("vehicle-map.json", vehicle_map)
        collector.write_json(
            "manifest.json",
            {
                "feature": "JNeoBattlespace",
                "scenarioId": scenario_id,
                "backend": backend.value,
                "runDir": str(run_dir),
                "headless": args.headless,
                "seed": args.seed,
                "startedAtUtc": ArtifactCollector.utc_now(),
            },
        )
        collector.write_json("versions.json", in_memory_versions(backend))
        collector.write_json(
            "process-manifest.json",
            {
                "backend": backend.value,
                "components": [],
                "childProcessesStarted": False,
                "reason": "IN_MEMORY backend uses no external simulator processes."
                if backend == Backend.IN_MEMORY
                else "Live process start is delegated to guarded orchestration.",
            },
        )
        controller = ScenarioController(
            backend=backend,
            definition=definition,
            collector=collector,
            clock=SimulationClock(seed=args.seed),
            vehicle_map=vehicle_map,
            headless=args.headless,
        )
        summaries.append(controller.run())

    write_suite_summary(output_root, backend, summaries)
    return 0 if all(summary["status"] == "PASS" for summary in summaries) else 1


def write_skipped_dependency_artifacts(
    output_root: Path,
    backend: Backend,
    scenario_id: str,
    dependency_check: dict,
    headless: bool,
    seed: int,
) -> dict:
    definition = definition_for(scenario_id)
    run_dir = output_root / backend.value.lower() / scenario_id
    collector = ArtifactCollector(run_dir)
    collector.touch_contract_files()
    vehicle_map = generate_vehicle_map(definition.vehicleCount, run_dir)
    firewall_report = GroundTruthFirewall().validate(default_agent_mappings(definition.vehicleCount))
    collector.write_json("vehicle-map.json", vehicle_map)
    collector.write_json(
        "manifest.json",
        {
            "feature": "JNeoBattlespace",
            "scenarioId": scenario_id,
            "backend": backend.value,
            "runDir": str(run_dir),
            "headless": headless,
            "seed": seed,
            "startedAtUtc": ArtifactCollector.utc_now(),
        },
    )
    collector.write_json("versions.json", dependency_check)
    collector.write_json(
        "process-manifest.json",
        {
            "backend": backend.value,
            "components": [
                "Gazebo Harmonic",
                "ArduPilot SITL",
                "ros_gz_bridge",
                "rosbridge_suite",
                "Jneopallium",
            ],
            "childProcessesStarted": False,
            "reason": "SKIPPED_DEPENDENCY",
        },
    )
    collector.write_json(
        "scenario-config.json",
        {
            "scenarioId": scenario_id,
            "backend": backend.value,
            "world": definition.world,
            "vehicleCount": definition.vehicleCount,
            "headless": headless,
        },
    )
    collector.write_json("sensor-topic-health.json", {"status": "SKIPPED_DEPENDENCY", "topics": []})
    collector.write_json("score.json", {"score": 0, "virtualEliminations": 0})
    collector.write_json("safety-summary.json", {"status": "SKIPPED_DEPENDENCY"})
    collector.write_json("ground-truth-firewall-report.json", firewall_report)
    summary = {
        "scenarioId": scenario_id,
        "backend": backend.value,
        "status": "SKIPPED_DEPENDENCY",
        "deterministic": False,
        "headless": headless,
        "missingDependencies": dependency_check["missing"],
        "runDir": str(run_dir),
    }
    collector.write_json("summary.json", summary)
    return summary


def write_live_not_started_artifacts(
    output_root: Path,
    backend: Backend,
    scenario_id: str,
    dependency_check: dict,
    headless: bool,
    seed: int,
) -> dict:
    summary = write_skipped_dependency_artifacts(output_root, backend, scenario_id, dependency_check, headless, seed)
    summary["status"] = "LIVE_NOT_STARTED"
    summary["missingDependencies"] = []
    summary["reason"] = "--allow-live-processes was not set"
    collector = ArtifactCollector(Path(summary["runDir"]))
    collector.write_json("summary.json", summary)
    return summary


def write_suite_summary(output_root: Path, backend: Backend, summaries: list[dict]) -> None:
    suite_dir = output_root / backend.value.lower()
    suite_dir.mkdir(parents=True, exist_ok=True)
    status = "PASS" if summaries and all(item["status"] == "PASS" for item in summaries) else "FAIL"
    if summaries and all(item["status"] == "SKIPPED_DEPENDENCY" for item in summaries):
        status = "SKIPPED_DEPENDENCY"
    payload = {
        "feature": "JNeoBattlespace",
        "backend": backend.value,
        "status": status,
        "scenarios": summaries,
        "generatedAtUtc": ArtifactCollector.utc_now(),
    }
    (suite_dir / "summary.json").write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def in_memory_versions(backend: Backend) -> dict:
    return {
        "backend": backend.value,
        "platform": platform.platform(),
        "python": platform.python_version(),
        "liveDependenciesRequired": backend == Backend.JNEO_BATTLESPACE,
        "note": "IN_MEMORY backend intentionally avoids Gazebo, ArduPilot SITL, ROS 2, and GStreamer.",
    }


def normalize_backend(value: str) -> Backend:
    normalized = value.strip().upper().replace("-", "_")
    if normalized in {"IN_MEMORY", "MEMORY"}:
        return Backend.IN_MEMORY
    if normalized in {"JNEO_BATTLESPACE", "JNEOBATTLESPACE", "BATTLESPACE"}:
        return Backend.JNEO_BATTLESPACE
    raise ValueError(f"unknown backend: {value}")


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run JNeoBattlespace scenarios.")
    parser.add_argument("scenario", nargs="?", default="live_single_autonomous", help="'all' or a scenario id")
    parser.add_argument("--backend", default="IN_MEMORY", help="IN_MEMORY or JNEO_BATTLESPACE")
    parser.add_argument("--headless", action="store_true", help="Run without GUI components.")
    parser.add_argument("--preview", action="store_true", help="Require GStreamer preview dependencies.")
    parser.add_argument("--allow-live-processes", action="store_true", help="Allow live Gazebo/SITL child process start.")
    parser.add_argument("--seed", type=int, default=24703042047, help="Deterministic seed.")
    parser.add_argument("--output", type=Path, default=None, help="Output root. Defaults to target/jneo-battlespace.")
    args = parser.parse_args(argv)
    if args.scenario != "all" and args.scenario not in scenario_ids():
        raise SystemExit(f"unknown scenario '{args.scenario}'. Valid: all, {', '.join(scenario_ids())}")
    return args


if __name__ == "__main__":
    raise SystemExit(main())

