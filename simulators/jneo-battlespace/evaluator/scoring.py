from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ScoreBoard:
    virtualEliminations: int = 0
    duplicatePhotographs: int = 0
    decoyPhotographs: int = 0
    rejectedPhotographs: int = 0

    def on_result(self, result: dict) -> None:
        status = result.get("status")
        if status == "ELIMINATED":
            self.virtualEliminations += 1
        elif status == "DUPLICATE":
            self.duplicatePhotographs += 1
        elif status == "DECOY":
            self.decoyPhotographs += 1
        else:
            self.rejectedPhotographs += 1

    def to_dict(self) -> dict:
        return {
            "virtualEliminations": self.virtualEliminations,
            "duplicatePhotographs": self.duplicatePhotographs,
            "decoyPhotographs": self.decoyPhotographs,
            "rejectedPhotographs": self.rejectedPhotographs,
            "score": self.virtualEliminations * 100 - self.decoyPhotographs * 10,
        }

