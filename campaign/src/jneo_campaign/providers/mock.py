from __future__ import annotations

import hashlib
from datetime import UTC, datetime
from typing import Any

from jneo_campaign.providers.interfaces import (
    CalendarSlot,
    OutboundEmail,
    ProviderMessage,
)


class MockGmailProvider:
    """Deterministic provider used by all dry runs and tests."""

    def __init__(self) -> None:
        self.sent: list[OutboundEmail] = []
        self.inbox: list[ProviderMessage] = []
        self.labels: dict[str, str] = {}

    def validate_credentials(self) -> dict[str, Any]:
        return {"provider": "mock-gmail", "valid": True, "external_writes": False}

    def ensure_labels(self, labels: list[str]) -> dict[str, str]:
        for label in labels:
            self.labels.setdefault(
                label, "mock-label-" + hashlib.sha256(label.encode()).hexdigest()[:12]
            )
        return dict(self.labels)

    def send(self, message: OutboundEmail, labels: list[str]) -> tuple[str, str]:
        for existing in self.sent:
            if existing.idempotency_key == message.idempotency_key:
                digest = hashlib.sha256(message.idempotency_key.encode()).hexdigest()[:20]
                return f"mock-message-{digest}", f"mock-thread-{digest}"
        self.sent.append(message)
        digest = hashlib.sha256(message.idempotency_key.encode()).hexdigest()[:20]
        return f"mock-message-{digest}", f"mock-thread-{digest}"

    def sync(self, query: str) -> list[ProviderMessage]:
        del query
        return list(self.inbox)

    def add_reply(
        self,
        *,
        thread_id: str,
        body: str,
        from_address: str = "recipient@example.org",
        subject: str = "Re: Jneopallium technical pilot",
    ) -> ProviderMessage:
        digest = hashlib.sha256(f"{thread_id}|{body}".encode()).hexdigest()[:20]
        item = ProviderMessage(
            message_id=f"mock-reply-{digest}",
            thread_id=thread_id,
            from_address=from_address,
            to_address="campaign@example.org",
            subject=subject,
            body_text=body,
            received_at=datetime.now(UTC),
            labels=("JNEOPALLIUM_CAMPAIGN", "JNEOPALLIUM_CAMPAIGN/REPLIED"),
        )
        self.inbox.append(item)
        return item


class MockCalendarProvider:
    def __init__(self) -> None:
        self.busy: list[CalendarSlot] = []
        self.events: dict[str, dict[str, Any]] = {}

    def validate_credentials(self) -> dict[str, Any]:
        return {"provider": "mock-calendar", "valid": True, "external_writes": False}

    def free_busy(self, start: datetime, end: datetime, calendar_id: str) -> list[CalendarSlot]:
        del calendar_id
        return [slot for slot in self.busy if slot.start < end and slot.end > start]

    def create_event(
        self,
        *,
        summary: str,
        description: str,
        start: datetime,
        end: datetime,
        timezone: str,
        attendees: list[str],
        idempotency_key: str,
    ) -> str:
        event_id = "mock-event-" + hashlib.sha256(idempotency_key.encode()).hexdigest()[:20]
        self.events.setdefault(
            event_id,
            {
                "summary": summary,
                "description": description,
                "start": start.isoformat(),
                "end": end.isoformat(),
                "timezone": timezone,
                "attendees": attendees,
            },
        )
        return event_id
