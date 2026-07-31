from __future__ import annotations

from sqlalchemy import select

from jneo_campaign.providers.interfaces import OutboundEmail
from jneo_campaign.providers.mock import MockCalendarProvider, MockGmailProvider
from jneo_campaign.storage.models import (
    EmailMessage,
    EmailThread,
    WorkflowState,
)
from tests.helpers import seed_campaign, seed_compliance_ready


def test_mock_gmail_is_idempotent() -> None:
    provider = MockGmailProvider()
    message = OutboundEmail(
        to="team@example.org",
        subject="Test",
        body_text="Text",
        body_html="<p>Text</p>",
        reply_to="reply@example.org",
        idempotency_key="same",
    )
    first = provider.send(message, ["JNEOPALLIUM_CAMPAIGN"])
    second = provider.send(message, ["JNEOPALLIUM_CAMPAIGN"])
    assert first == second
    assert len(provider.sent) == 1


def test_calendar_event_requires_agreed_option(runner) -> None:
    assert isinstance(runner.providers.calendar, MockCalendarProvider)
    with runner.database.session() as session:
        organization, contact, _asset = seed_compliance_ready(session)
        campaign = seed_campaign(session)
        state = session.scalar(select(WorkflowState))
        state.state = "MEETING_REQUESTED"
        thread = EmailThread(
            campaign_id=campaign.id,
            organization_id=organization.id,
            contact_id=contact.id,
            provider_thread_id="mock-thread-1",
            subject="Technical review",
            status="DELIVERED",
        )
        session.add(thread)
        session.flush()
        meetings = runner.meetings.propose(session)
        assert len(meetings) == 1
        assert runner.providers.calendar.events == {}
        agreement = EmailMessage(
            thread_id=thread.id,
            direction="INBOUND",
            sequence_number=0,
            subject="Re",
            body_text="I agree to option 1.",
            body_html="<p>I agree to option 1.</p>",
            status="RECEIVED",
            idempotency_key="agreement",
        )
        session.add(agreement)
        session.flush()
        scheduled = runner.meetings.confirm_agreements(session)
        assert len(scheduled) == 1
        assert scheduled[0].recipient_agreed
        assert len(runner.providers.calendar.events) == 1


def test_timezone_options_are_in_both_business_windows(runner) -> None:
    slots = runner.meetings._three_slots("America/New_York")
    assert len(slots) == 3
    assert all(slot.start.tzinfo is not None for slot in slots)
