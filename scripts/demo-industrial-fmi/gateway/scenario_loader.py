from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List

import yaml


@dataclass(frozen=True)
class ScenarioEvent:
    at: float
    set: Dict[str, Any] = field(default_factory=dict)
    opcua: Dict[str, Any] = field(default_factory=dict)
    mqtt_available: bool | None = None
    opcua_available: bool | None = None


@dataclass(frozen=True)
class Scenario:
    name: str
    durationSeconds: float
    initial: Dict[str, Any] = field(default_factory=dict)
    events: List[ScenarioEvent] = field(default_factory=list)
    controllerMode: str = "SHADOW"


def load_scenario(path: Path) -> Scenario:
    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    events = [
        ScenarioEvent(
            at=float(item.get("at", 0.0)),
            set=dict(item.get("set", {}) or {}),
            opcua=dict(item.get("opcua", {}) or {}),
            mqtt_available=item.get("mqttAvailable"),
            opcua_available=item.get("opcuaAvailable"),
        )
        for item in data.get("events", []) or []
    ]
    return Scenario(
        name=data.get("name", path.stem),
        durationSeconds=float(data.get("durationSeconds", 60.0)),
        initial=dict(data.get("initial", {}) or {}),
        events=events,
        controllerMode=str(data.get("controllerMode", "SHADOW")),
    )
