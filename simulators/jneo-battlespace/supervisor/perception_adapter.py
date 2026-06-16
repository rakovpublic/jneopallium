from __future__ import annotations

import hashlib
from dataclasses import asdict, dataclass
from typing import Protocol


@dataclass(frozen=True)
class FpvDetection:
    trackId: str
    confidence: float
    bbox: list[float]
    classLabel: str
    source: str

    def public_dict(self) -> dict:
        return asdict(self)


class FpvPerceptionAdapter(Protocol):
    def detect(self, frame_bytes: bytes, uav_id: str, simulation_time: float) -> list[FpvDetection]:
        """Detect public image-space observations from pixels only."""


class DeterministicSyntheticPerceptionAdapter:
    """Pixel-token detector used by the deterministic backend."""

    def detect(self, frame_bytes: bytes, uav_id: str, simulation_time: float) -> list[FpvDetection]:
        text = frame_bytes.decode("utf-8", errors="ignore")
        digest = hashlib.sha256(frame_bytes + uav_id.encode("utf-8")).hexdigest()[:10]
        if "TARGET_VISIBLE" in text:
            return [
                FpvDetection(
                    trackId=f"track-{digest}",
                    confidence=0.91,
                    bbox=[0.38, 0.31, 0.24, 0.22],
                    classLabel="virtual-target-candidate",
                    source="deterministic-pixels",
                )
            ]
        if "INFANTRY_VISIBLE" in text:
            return [
                FpvDetection(
                    trackId=f"track-infantry-{digest}",
                    confidence=0.88,
                    bbox=[0.43, 0.28, 0.12, 0.30],
                    classLabel="infantry",
                    source="deterministic-pixels",
                )
            ]
        if "VEHICLE_VISIBLE" in text:
            return [
                FpvDetection(
                    trackId=f"track-vehicle-{digest}",
                    confidence=0.90,
                    bbox=[0.31, 0.39, 0.38, 0.18],
                    classLabel="vehicle",
                    source="deterministic-pixels",
                )
            ]
        if "DECOY_VISIBLE" in text:
            return [
                FpvDetection(
                    trackId=f"track-{digest}",
                    confidence=0.54,
                    bbox=[0.41, 0.34, 0.21, 0.19],
                    classLabel="target-like-decoy",
                    source="deterministic-pixels",
                )
            ]
        return []


class GazeboFramePerceptionAdapter:
    """Simple initial live adapter that operates only on image bytes.

    It intentionally has no Gazebo entity-state dependency. The detector is a
    placeholder for a learned model: it looks for simulator target texture
    bytes or a high-contrast fiducial-like color signature in the frame.
    """

    def detect(self, frame_bytes: bytes, uav_id: str, simulation_time: float) -> list[FpvDetection]:
        if not frame_bytes:
            return []
        digest = hashlib.sha256(frame_bytes).hexdigest()[:10]
        marker_score = frame_bytes.count(b"JNEO_TARGET") / max(1, len(frame_bytes))
        high_contrast_score = self._high_contrast_score(frame_bytes)
        confidence = max(marker_score * 1000.0, high_contrast_score)
        if confidence < 0.35:
            return []
        return [
            FpvDetection(
                trackId=f"gazebo-track-{digest}",
                confidence=min(0.96, round(confidence, 3)),
                bbox=[0.35, 0.30, 0.30, 0.26],
                classLabel="virtual-target-candidate",
                source="gazebo-frame-bytes",
            )
        ]

    @staticmethod
    def _high_contrast_score(frame_bytes: bytes) -> float:
        sample = frame_bytes[:8192]
        if not sample:
            return 0.0
        lo = sum(1 for value in sample if value < 32)
        hi = sum(1 for value in sample if value > 224)
        return min(lo, hi) / max(1, len(sample))
