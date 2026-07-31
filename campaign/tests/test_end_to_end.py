from __future__ import annotations


def test_complete_dry_run_acceptance_scenario(runner) -> None:
    result = runner.run_once(simulate_replies=True)
    assert all(stage["status"] == "COMPLETED" for stage in result["stages"].values())
    metrics = result["acceptance"]
    assert metrics["organizations_discovered"] >= 25
    assert metrics["organizations_verified"] >= 25
    assert metrics["verified_contacts"] >= 10
    assert metrics["prospects_scored"] >= 25
    assert metrics["messages_prepared"] >= 5
    assert metrics["mock_messages_sent"] >= 5
    assert metrics["real_external_messages_sent"] == 0
    assert metrics["reply_rate"] > 0
    assert metrics["meeting_rate"] > 0
    assert metrics["provider_failures"] == 0
    assert all(
        {"prospects", "sent", "replied", "positive_replies", "positive_conversion_rate"}
        <= set(bucket)
        for bucket in metrics["conversion_by_domain"].values()
    )
    assert metrics["conversion_by_persona"]
