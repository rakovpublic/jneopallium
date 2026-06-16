from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


class ArtifactCollector:
    """Writes the public and protected artifact contract for a run."""

    PUBLIC_JSON = {
        "manifest.json",
        "versions.json",
        "process-manifest.json",
        "vehicle-map.json",
        "scenario-config.json",
        "sensor-topic-health.json",
        "score.json",
        "safety-summary.json",
        "ground-truth-firewall-report.json",
        "summary.json",
    }

    PUBLIC_JSONL = {
        "per-uav-camera-events.jsonl",
        "mavlink-events.jsonl",
        "communication-events.jsonl",
        "flight-intents.jsonl",
        "command-audit.jsonl",
        "photograph-submissions.jsonl",
        "photograph-results.jsonl",
        "virtual-eliminations.jsonl",
    }

    PUBLIC_LOGS = {
        "gazebo.log",
        "per-vehicle-sitl.log",
        "ros-gz-bridge.log",
        "rosbridge.log",
        "jneopallium.log",
    }

    def __init__(self, run_dir: Path) -> None:
        self.run_dir = run_dir
        self.protected_dir = run_dir / "protected-evaluator"
        self.run_dir.mkdir(parents=True, exist_ok=True)
        self.protected_dir.mkdir(parents=True, exist_ok=True)

    def write_json(self, name: str, payload: Any) -> Path:
        path = self.run_dir / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        return path

    def append_jsonl(self, name: str, payload: Any) -> Path:
        path = self.run_dir / name
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(payload, sort_keys=True) + "\n")
        return path

    def write_log(self, name: str, text: str) -> Path:
        path = self.run_dir / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        return path

    def append_protected_jsonl(self, name: str, payload: Any) -> Path:
        path = self.protected_dir / name
        with path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(payload, sort_keys=True) + "\n")
        return path

    def touch_contract_files(self) -> None:
        for name in self.PUBLIC_JSONL:
            path = self.run_dir / name
            if not path.exists():
                path.write_text("", encoding="utf-8")
        for name in self.PUBLIC_LOGS:
            path = self.run_dir / name
            if not path.exists():
                path.write_text("not started in this backend\n", encoding="utf-8")

    @staticmethod
    def utc_now() -> str:
        return datetime.now(timezone.utc).isoformat()

