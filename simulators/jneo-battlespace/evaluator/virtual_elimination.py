from __future__ import annotations

from dataclasses import asdict, dataclass
from threading import Lock


@dataclass
class VirtualTarget:
    targetId: str
    visualToken: str
    active: bool = True
    eliminatedBySubmission: str | None = None
    eliminatedAtSimulationTime: float | None = None


class VirtualEliminationLedger:
    """Atomic target lifecycle ledger for photo scoring."""

    def __init__(self, targets: list[VirtualTarget]) -> None:
        self._targets = {target.targetId: target for target in targets}
        self._lock = Lock()

    def active_targets(self) -> list[dict]:
        with self._lock:
            return [asdict(target) for target in self._targets.values() if target.active]

    def eliminate_once(self, target_id: str, submission_id: str, simulation_time: float) -> tuple[bool, dict]:
        with self._lock:
            target = self._targets.get(target_id)
            if target is None:
                return False, {"targetId": target_id, "reason": "UNKNOWN_TARGET"}
            if not target.active:
                return False, {
                    "targetId": target_id,
                    "reason": "ALREADY_ELIMINATED",
                    "eliminatedBySubmission": target.eliminatedBySubmission,
                }
            target.active = False
            target.eliminatedBySubmission = submission_id
            target.eliminatedAtSimulationTime = simulation_time
            return True, asdict(target)

