from __future__ import annotations

from fastapi.testclient import TestClient

from jneo_campaign.dashboard.app import create_app
from tests.helpers import seed_campaign


def test_dashboard_health_and_campaign_pause(runner, settings) -> None:
    with runner.database.session() as session:
        seed_campaign(session)

    client = TestClient(create_app(settings))
    health = client.get("/api/health")
    assert health.status_code == 200
    assert health.json()["status"] == "ok"
    assert health.json()["live_writes_enabled"] is False

    pause = client.post(
        "/api/pause/CAMPAIGN/dry-run-pilot",
        json={"paused": True, "reason": "dashboard unit test"},
    )
    assert pause.status_code == 200
    assert pause.json()["paused"] is True
    assert client.get("/api/health").json()["status"] == "paused"
