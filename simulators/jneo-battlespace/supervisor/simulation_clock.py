from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone


@dataclass
class SimulationClock:
    """Authoritative scenario clock abstraction.

    The live backend is expected to advance this from Gazebo /clock. The
    deterministic backend advances it locally with a fixed step and seed.
    """

    seed: int
    step_seconds: float = 0.25
    sim_time_seconds: float = 0.0
    reset_generation: int = 0
    started_wall_utc: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def stamp(self) -> dict:
        return {
            "simulationTime": round(self.sim_time_seconds, 6),
            "wallTimeUtc": datetime.now(timezone.utc).isoformat(),
            "resetGeneration": self.reset_generation,
        }

    def advance(self, steps: int = 1) -> dict:
        if steps < 0:
            raise ValueError("steps must be non-negative")
        self.sim_time_seconds += self.step_seconds * steps
        return self.stamp()

    def reset(self) -> dict:
        self.sim_time_seconds = 0.0
        self.reset_generation += 1
        return self.stamp()

