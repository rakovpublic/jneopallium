from __future__ import annotations

import hashlib
from datetime import UTC, datetime, timedelta

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.config import CampaignPolicy, Settings
from jneo_campaign.providers.interfaces import GmailProvider, OutboundEmail
from jneo_campaign.security import text_to_safe_html
from jneo_campaign.storage.models import (
    Contact,
    EmailMessage,
    EmailThread,
    FollowUp,
    GeneratedAsset,
    Organization,
    SuppressionEntry,
)


class FollowUpService:
    def __init__(self, settings: Settings, policy: CampaignPolicy, provider: GmailProvider) -> None:
        self.settings = settings
        self.policy = policy
        self.provider = provider

    def schedule(self, session: Session) -> list[FollowUp]:
        created: list[FollowUp] = []
        for thread in session.scalars(
            select(EmailThread).where(EmailThread.status.in_(["MOCK_SENT", "SENT", "DELIVERED"]))
        ):
            if thread.sequence_paused or self._has_reply(session, thread.id):
                continue
            initial = session.scalar(
                select(EmailMessage).where(
                    EmailMessage.thread_id == thread.id,
                    EmailMessage.direction == "OUTBOUND",
                    EmailMessage.sequence_number == 0,
                )
            )
            if initial is None or initial.sent_at is None:
                continue
            for sequence in range(1, self.policy.limits.max_automated_followups + 1):
                item = session.scalar(
                    select(FollowUp).where(
                        FollowUp.thread_id == thread.id,
                        FollowUp.sequence_number == sequence,
                    )
                )
                if item is None:
                    due = self._add_business_days(
                        initial.sent_at,
                        self.policy.limits.min_business_days_between_followups * sequence,
                    )
                    item = FollowUp(
                        thread_id=thread.id,
                        sequence_number=sequence,
                        due_at=due,
                        value_add_type="integration_artifact"
                        if sequence == 1
                        else "evaluation_boundary",
                    )
                    session.add(item)
                    created.append(item)
        session.flush()
        return created

    def run(self, session: Session) -> list[EmailMessage]:
        sent: list[EmailMessage] = []
        now = datetime.now(UTC)
        for followup in session.scalars(
            select(FollowUp)
            .where(FollowUp.status == "SCHEDULED", FollowUp.due_at <= now)
            .order_by(FollowUp.due_at)
        ):
            thread = session.get(EmailThread, followup.thread_id)
            if thread is None or thread.sequence_paused or self._has_reply(session, thread.id):
                followup.status = "STOPPED"
                continue
            organization = session.get(Organization, thread.organization_id)
            contact = session.get(Contact, thread.contact_id)
            if not organization or not contact or self._suppressed(session, organization, contact):
                followup.status = "STOPPED"
                continue
            asset = session.scalar(
                select(GeneratedAsset).where(
                    GeneratedAsset.organization_id == organization.id,
                    GeneratedAsset.asset_type == "follow_up_sequence",
                    GeneratedAsset.format == "markdown",
                )
            )
            if asset is None:
                followup.status = "ERROR"
                continue
            section = self._section(asset.content, followup.sequence_number)
            key = hashlib.sha256(
                f"{thread.id}|followup|{followup.sequence_number}".encode()
            ).hexdigest()
            message = session.scalar(
                select(EmailMessage).where(EmailMessage.idempotency_key == key)
            )
            if message is None:
                message = EmailMessage(
                    thread_id=thread.id,
                    direction="OUTBOUND",
                    sequence_number=followup.sequence_number,
                    subject=f"Re: {thread.subject}",
                    body_text=section,
                    body_html=text_to_safe_html(section),
                    status="QUEUED",
                    scheduled_for=followup.due_at,
                    idempotency_key=key,
                )
                session.add(message)
                session.flush()
            provider_message_id, provider_thread_id = self.provider.send(
                OutboundEmail(
                    to=contact.channel_value,
                    subject=message.subject,
                    body_text=message.body_text,
                    body_html=message.body_html,
                    reply_to=self.settings.campaign_reply_to or "dry-run@example.invalid",
                    idempotency_key=key,
                    thread_id=thread.provider_thread_id,
                ),
                [
                    "JNEOPALLIUM_CAMPAIGN",
                    "JNEOPALLIUM_CAMPAIGN/OUTBOUND",
                    "JNEOPALLIUM_CAMPAIGN/WAITING",
                ],
            )
            message.provider_message_id = provider_message_id
            message.sent_at = now
            message.status = "SENT" if self.settings.live_writes_enabled else "MOCK_SENT"
            thread.provider_thread_id = provider_thread_id
            followup.status = "SENT"
            sent.append(message)
        return sent

    @staticmethod
    def _add_business_days(start: datetime, days: int) -> datetime:
        result = start
        added = 0
        while added < days:
            result += timedelta(days=1)
            if result.weekday() < 5:
                added += 1
        return result

    @staticmethod
    def _has_reply(session: Session, thread_id: int) -> bool:
        return (
            session.scalar(
                select(EmailMessage.id)
                .where(
                    EmailMessage.thread_id == thread_id,
                    EmailMessage.direction == "INBOUND",
                )
                .limit(1)
            )
            is not None
        )

    @staticmethod
    def _section(content: str, sequence: int) -> str:
        marker = f"## Follow-up {sequence}"
        if marker not in content:
            return content
        tail = content.split(marker, 1)[1]
        return tail.split("## Follow-up", 1)[0].strip()

    @staticmethod
    def _suppressed(session: Session, organization: Organization, contact: Contact) -> bool:
        values = {organization.canonical_domain.lower(), contact.channel_value.lower()}
        return (
            session.scalar(
                select(SuppressionEntry.id)
                .where(
                    SuppressionEntry.active.is_(True),
                    SuppressionEntry.normalized_value.in_(values),
                )
                .limit(1)
            )
            is not None
        )
