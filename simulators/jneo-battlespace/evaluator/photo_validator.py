from __future__ import annotations

import hashlib
from dataclasses import asdict, dataclass

from evaluator.virtual_elimination import VirtualEliminationLedger
from supervisor.artifact_collector import ArtifactCollector


@dataclass(frozen=True)
class PhotoSubmission:
    submissionId: str
    uavId: str
    cameraTopic: str
    frameId: str
    trackId: str
    simulationTime: float
    imageDigest: str
    resetGeneration: int

    def public_dict(self) -> dict:
        return asdict(self)


class PhotoValidator:
    """Evaluator-side photo validator with protected evidence output."""

    def __init__(self, ledger: VirtualEliminationLedger, collector: ArtifactCollector) -> None:
        self.ledger = ledger
        self.collector = collector

    def validate(self, submission: PhotoSubmission, frame_bytes: bytes, scenario_private: dict) -> dict:
        frame_digest = hashlib.sha256(frame_bytes).hexdigest()
        token = frame_bytes.decode("utf-8", errors="ignore")
        protected = {
            "submissionId": submission.submissionId,
            "frameDigest": frame_digest,
            "cameraTopic": submission.cameraTopic,
            "simulationTime": submission.simulationTime,
            "cameraPose": scenario_private.get("cameraPoseByUav", {}).get(submission.uavId),
            "targetState": scenario_private.get("targets"),
            "visibility": scenario_private.get("visibility"),
            "occlusion": scenario_private.get("occlusion"),
        }
        self.collector.append_protected_jsonl("photo-evidence.jsonl", protected)

        if submission.imageDigest != frame_digest:
            return self._public_result(submission, "REJECTED", "FRAME_DIGEST_MISMATCH")
        if "DECOY_VISIBLE" in token:
            return self._public_result(submission, "DECOY", "NO_ACTIVE_TARGET_VALIDATED")
        if not self._frame_contains_known_target(token, scenario_private):
            return self._public_result(submission, "REJECTED", "TARGET_NOT_VISIBLE")

        target_id = self._target_id_for_frame(token, scenario_private)
        eliminated, detail = self.ledger.eliminate_once(target_id, submission.submissionId, submission.simulationTime)
        if eliminated:
            public = self._public_result(submission, "ELIMINATED", "PHOTO_VALIDATED")
            public["award"] = 100
            self.collector.append_jsonl(
                "virtual-eliminations.jsonl",
                {
                    "submissionId": submission.submissionId,
                    "uavId": submission.uavId,
                    "simulationTime": submission.simulationTime,
                    "publicOutcome": "ELIMINATED",
                },
            )
            return public
        if detail.get("reason") == "ALREADY_ELIMINATED":
            return self._public_result(submission, "DUPLICATE", "TARGET_ALREADY_ELIMINATED")
        return self._public_result(submission, "REJECTED", detail.get("reason", "UNKNOWN_TARGET"))

    @staticmethod
    def _public_result(submission: PhotoSubmission, status: str, reason: str) -> dict:
        return {
            "submissionId": submission.submissionId,
            "uavId": submission.uavId,
            "frameId": submission.frameId,
            "trackId": submission.trackId,
            "simulationTime": submission.simulationTime,
            "status": status,
            "reason": reason,
        }

    @staticmethod
    def _target_id_for_frame(token: str, scenario_private: dict) -> str:
        for target in scenario_private.get("targets", []):
            visual_token = target.get("visualToken")
            if visual_token and visual_token in token:
                return target.get("targetId", scenario_private.get("primaryTargetId", "target-1"))
        return scenario_private.get("primaryTargetId", "target-1")

    @staticmethod
    def _frame_contains_known_target(token: str, scenario_private: dict) -> bool:
        for target in scenario_private.get("targets", []):
            visual_token = target.get("visualToken")
            if visual_token and visual_token in token:
                return True
        return "TARGET_VISIBLE" in token
