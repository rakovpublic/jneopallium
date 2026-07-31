from __future__ import annotations

from datetime import UTC, datetime

from sqlalchemy import func, select

from jneo_campaign.storage.models import ProviderFailure


def test_stage_retries_with_exponential_policy(runner) -> None:
    attempts = {"count": 0}

    def flaky(_session):
        attempts["count"] += 1
        if attempts["count"] < 3:
            raise RuntimeError("temporary provider failure")
        return {"ok": True}

    result = runner._execute_stage("flaky-test", datetime.now(UTC).isoformat(), flaky)
    assert result["status"] == "COMPLETED"
    assert attempts["count"] == 3


def test_failed_provider_does_not_block_next_stage(runner) -> None:
    cycle = datetime.now(UTC).isoformat()

    def failed(_session):
        raise RuntimeError("provider unavailable api_key=secret")

    first = runner._execute_stage("failed-provider", cycle, failed)
    second = runner._execute_stage("independent-analytics", cycle, lambda _session: {"ok": True})
    assert first["status"] == "FAILED"
    assert first["continued"]
    assert second["status"] == "COMPLETED"
    with runner.database.session() as session:
        assert session.scalar(select(func.count(ProviderFailure.id))) == 1
