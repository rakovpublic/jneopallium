from __future__ import annotations

from sqlalchemy import select

from jneo_campaign.asset_generation.service import AssetGenerationService
from jneo_campaign.storage.models import EmailMessage


def test_robotics_topic_is_not_misclassified_as_bot_detection() -> None:
    subject = AssetGenerationService._subject_line(
        "MassRobotics", "Mobile and industrial robotics"
    )

    assert subject == "Reproducible autonomy test plan for MassRobotics"


def test_relevance_clause_uses_plural_verb_agreement() -> None:
    clause = AssetGenerationService._relevance_clause(
        "The official page describes testbeds and proof-of-concept services.",
        "A technical organization.",
    )

    assert clause == "your public materials describe testbeds and proof-of-concept services"


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
        "List-Unsubscribe" not in message.headers for message in runner.providers.gmail.sent
    )
    assert all(
        {"prospects", "sent", "replied", "positive_replies", "positive_conversion_rate"}
        <= set(bucket)
        for bucket in metrics["conversion_by_domain"].values()
    )
    assert metrics["conversion_by_persona"]
    with runner.database.session() as session:
        messages = list(
            session.scalars(
                select(EmailMessage).where(
                    EmailMessage.direction == "OUTBOUND", EmailMessage.sequence_number == 0
                )
            )
        )
        assert all("would use Generate" not in message.body_text for message in messages)
        assert all(
            "Repository: https://github.com/rakovpublic/jneopallium" in message.body_text
            for message in messages
        )
        assert all("[source:" not in message.body_text.lower() for message in messages)
        assert all("scope limits:" not in message.body_text.lower() for message in messages)
        assert all("bounded technical evaluation" not in message.body_text.lower() for message in messages)
        assert all("multi-timescale" not in message.body_text.lower() for message in messages)
        assert all("decision jsonl" not in message.body_text.lower() for message in messages)
        assert all(
            "Would it be useful if I sent that plan first?" in message.body_text
            for message in messages
        )
        assert all(
            "Jneopallium is an open-source Java research framework" in message.body_text
            for message in messages
        )
        assert all(len(message.body_text.split()) <= 200 for message in messages)
