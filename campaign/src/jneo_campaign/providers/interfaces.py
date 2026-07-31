from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any, Protocol


@dataclass(frozen=True)
class SearchFact:
    organization_name: str
    canonical_domain: str
    organization_type: str
    prospect_category: str
    target_domain: str
    region: str
    country: str | None
    summary: str
    source_url: str
    source_type: str
    supporting_excerpt: str
    contact_role: str | None = None
    contact_channel_type: str | None = None
    contact_channel_value: str | None = None
    contact_source_url: str | None = None
    contact_supporting_excerpt: str | None = None
    contact_timezone: str | None = None


class SearchProvider(Protocol):
    name: str

    def discover(self, domains: list[str], limit: int) -> list[SearchFact]: ...


@dataclass(frozen=True)
class OutboundEmail:
    to: str
    subject: str
    body_text: str
    body_html: str
    reply_to: str
    idempotency_key: str
    thread_id: str | None = None
    headers: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class ProviderMessage:
    message_id: str
    thread_id: str
    from_address: str
    to_address: str
    subject: str
    body_text: str
    received_at: datetime
    labels: tuple[str, ...] = ()


class GmailProvider(Protocol):
    def validate_credentials(self) -> dict[str, Any]: ...
    def ensure_labels(self, labels: list[str]) -> dict[str, str]: ...
    def send(self, message: OutboundEmail, labels: list[str]) -> tuple[str, str]: ...
    def sync(self, query: str) -> list[ProviderMessage]: ...


@dataclass(frozen=True)
class CalendarSlot:
    start: datetime
    end: datetime


class CalendarProvider(Protocol):
    def validate_credentials(self) -> dict[str, Any]: ...
    def free_busy(self, start: datetime, end: datetime, calendar_id: str) -> list[CalendarSlot]: ...
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
    ) -> str: ...


class StructuredLLMProvider(Protocol):
    name: str

    def generate(self, *, schema: type[Any], task: str, evidence: list[dict[str, Any]]) -> Any: ...
