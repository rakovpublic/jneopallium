from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class NetworkFault:
    faultId: str
    kind: str
    affectedNodes: list[str]
    startSimulationTime: float
    endSimulationTime: float | None = None


def active_disabled_nodes(faults: list[NetworkFault], simulation_time: float) -> set[str]:
    disabled: set[str] = set()
    for fault in faults:
        if fault.kind != "NODE_DOWN":
            continue
        if fault.startSimulationTime <= simulation_time and (
            fault.endSimulationTime is None or simulation_time <= fault.endSimulationTime
        ):
            disabled.update(fault.affectedNodes)
    return disabled

