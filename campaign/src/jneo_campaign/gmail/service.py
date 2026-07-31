from __future__ import annotations

import hashlib
from datetime import UTC, datetime, timedelta
from zoneinfo import ZoneInfo

from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session

from jneo_campaign.config import CampaignPolicy, Settings
from jneo_campaign.providers.interfaces import GmailProvider, OutboundEmail
from jneo_campaign.security import text_to_safe_html
from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Campaign,
    ComplianceDecision,
    Contact,
    Domain,
    EmailMessage,
    EmailThread,
    GeneratedAsset,
    Organization,
    PauseControl,
    ReplyClassification,
    SuppressionEntry,
)

LABELS = [
    "JNEOPALLIUM_CAMPAIGN",
    "JNEOPALLIUM_CAMPAIGN/OUTBOUND",
    "JNEOPALLIUM_CAMPAIGN/REPLIED",
    "JNEOPALLIUM_CAMPAIGN/POSITIVE",
    "JNEOPALLIUM_CAMPAIGN/WAITING",
    "JNEOPALLIUM_CAMPAIGN/MEETING",
    "JNEOPALLIUM_CAMPAIGN/DO_NOT_CONTACT",
    "JNEOPALLIUM_CAMPAIGN/ERROR",
]


class GmailOutreachService:
    def __init__(
        self,
        settings: Settings,
        policy: CampaignPolicy,
        provider: GmailProvider,
    ) -> None:
        self.settings = settings
        self.policy = policy
        self.provider = provider

    def prepare(self, session: Session) -> list[EmailMessage]:
        campaign = self._campaign(session)
        if campaign.paused:
            return []
        prepared: list[EmailMessage] = []
        approved = list(
            session.scalars(
                select(ComplianceDecision).where(
                    ComplianceDecision.decision.in_(["APPROVED", "APPROVED_DRY_RUN"])
                )
            )
        )
        for decision in approved:
            organization = session.get(Organization, decision.organization_id)
            contact = session.get(Contact, decision.contact_id)
            asset = session.get(GeneratedAsset, decision.asset_id)
            if not organization or not contact or not asset or organization.paused:
                continue
            if contact.channel_type != "email" or self._suppressed(session, organization, contact):
                continue
            thread = session.scalar(
                select(EmailThread).where(
                    EmailThread.campaign_id == campaign.id,
                    EmailThread.organization_id == organization.id,
                    EmailThread.contact_id == contact.id,
                )
            )
            subject, body = self._split_subject(asset.content)
            if thread is None:
                thread = EmailThread(
                    campaign_id=campaign.id,
                    organization_id=organization.id,
                    contact_id=contact.id,
                    subject=subject,
                    status="QUEUED",
                )
                session.add(thread)
                session.flush()
            key = hashlib.sha256(
                f"{campaign.campaign_key}|{organization.id}|{contact.id}|initial".encode()
            ).hexdigest()
            message = session.scalar(
                select(EmailMessage).where(EmailMessage.idempotency_key == key)
            )
            if message is None:
                finalized = self._finalize_body(body)
                message = EmailMessage(
                    thread_id=thread.id,
                    direction="OUTBOUND",
                    sequence_number=0,
                    subject=subject,
                    body_text=finalized,
                    body_html=text_to_safe_html(finalized),
                    status="QUEUED",
                    scheduled_for=self._scheduled_time(contact, key),
                    idempotency_key=key,
                )
                session.add(message)
                prepared.append(message)
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.QUEUED,
                reason="Compliance-approved initial message placed in idempotent queue",
                source=f"outreach-queue:{key}",
            )
        session.flush()
        return prepared

    def send(self, session: Session) -> list[EmailMessage]:
        campaign = self._campaign(session)
        if campaign.paused:
            return []
        self.provider.ensure_labels(LABELS)
        if self._halted_by_health(session):
            return []
        start = datetime.now(UTC).replace(hour=0, minute=0, second=0, microsecond=0)
        sent_today = (
            session.scalar(
                select(func.count(EmailMessage.id)).where(
                    EmailMessage.direction == "OUTBOUND",
                    EmailMessage.sent_at >= start,
                    EmailMessage.status.in_(["SENT", "MOCK_SENT", "DELIVERED"]),
                )
            )
            or 0
        )
        capacity = max(0, self.policy.limits.max_outbound_per_day - sent_today)
        now = datetime.now(UTC)
        queue = list(
            session.scalars(
                select(EmailMessage)
                .where(
                    EmailMessage.direction == "OUTBOUND",
                    EmailMessage.sequence_number == 0,
                    EmailMessage.status == "QUEUED",
                    or_(EmailMessage.scheduled_for.is_(None), EmailMessage.scheduled_for <= now),
                )
                .order_by(EmailMessage.scheduled_for, EmailMessage.id)
                .limit(min(capacity, self.policy.limits.max_new_contacts_per_day))
            )
        )
        sent: list[EmailMessage] = []
        for message in queue:
            thread = session.get(EmailThread, message.thread_id)
            if thread is None or thread.sequence_paused:
                continue
            organization = session.get(Organization, thread.organization_id)
            contact = session.get(Contact, thread.contact_id)
            if (
                not organization
                or not contact
                or self._suppressed(session, organization, contact)
                or self._paused(session, organization, thread)
            ):
                message.status = "SUPPRESSED"
                continue
            if self.settings.live_writes_enabled and not self._inside_business_window(contact):
                continue
            provider_message_id, provider_thread_id = self.provider.send(
                OutboundEmail(
                    to=contact.channel_value,
                    subject=message.subject,
                    body_text=message.body_text,
                    body_html=message.body_html,
                    reply_to=self.settings.campaign_reply_to or "dry-run@example.invalid",
                    idempotency_key=message.idempotency_key,
                    thread_id=thread.provider_thread_id,
                    headers={
                        "List-Unsubscribe": f"<mailto:{self.settings.campaign_reply_to or 'dry-run@example.invalid'}?subject=unsubscribe>"
                    },
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
            thread.status = message.status
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.CONTACTED,
                reason="Initial message sent through configured provider"
                if self.settings.live_writes_enabled
                else "Initial message simulated through mock Gmail",
                source=f"gmail:{provider_message_id}",
            )
            if not self.settings.live_writes_enabled:
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.DELIVERED,
                    reason="Mock provider deterministically simulates delivery",
                    source=f"gmail:{provider_message_id}",
                )
                message.delivered_at = now
                thread.status = "DELIVERED"
            sent.append(message)
        return sent

    def _campaign(self, session: Session) -> Campaign:
        item = session.scalar(
            select(Campaign).where(Campaign.campaign_key == self.policy.campaign_id)
        )
        snapshot = self.policy.model_dump(mode="json")
        if item is None:
            item = Campaign(
                campaign_key=self.policy.campaign_id,
                name="Jneopallium evidence-grounded pilot",
                audience="mixed-pilot",
                config_snapshot=snapshot,
            )
            session.add(item)
            session.flush()
        else:
            item.config_snapshot = snapshot
        return item

    @staticmethod
    def _split_subject(content: str) -> tuple[str, str]:
        lines = content.splitlines()
        if lines and lines[0].lower().startswith("subject:"):
            return lines[0].split(":", 1)[1].strip(), "\n".join(lines[1:]).strip()
        return "A bounded Jneopallium technical evaluation", content

    def _finalize_body(self, body: str) -> str:
        if self.settings.live_writes_enabled:
            identity = (
                f"{self.settings.campaign_sender_name}\n{self.settings.campaign_organization}\n"
                f"{self.settings.campaign_postal_address}"
            )
        else:
            identity = "Jneopallium campaign DRY_RUN\nNo external email is sent. Configure identity before LIVE."
        return body.replace(
            "Jneopallium campaign team\nOpen-source technical research project\nSender identity and postal address must be configured before LIVE sending.",
            identity,
        )

    def _scheduled_time(self, contact: Contact, key: str) -> datetime:
        now = datetime.now(UTC)
        if not self.settings.live_writes_enabled:
            return now
        zone = ZoneInfo(contact.timezone or self.settings.campaign_default_timezone)
        local = now.astimezone(zone)
        offset_minutes = int(key[:4], 16) % max(
            1, (self.policy.send_window_end - self.policy.send_window_start) * 60
        )
        candidate = local.replace(
            hour=self.policy.send_window_start, minute=0, second=0, microsecond=0
        ) + timedelta(minutes=offset_minutes)
        if candidate <= local:
            candidate += timedelta(days=1)
        while candidate.weekday() >= 5:
            candidate += timedelta(days=1)
        return candidate.astimezone(UTC)

    def _inside_business_window(self, contact: Contact) -> bool:
        if not contact.timezone:
            return False
        local = datetime.now(UTC).astimezone(ZoneInfo(contact.timezone))
        return (
            local.weekday() < 5
            and self.policy.send_window_start <= local.hour < self.policy.send_window_end
        )

    @staticmethod
    def _suppressed(session: Session, organization: Organization, contact: Contact) -> bool:
        return (
            session.scalar(
                select(SuppressionEntry).where(
                    SuppressionEntry.active.is_(True),
                    or_(
                        SuppressionEntry.normalized_value == contact.channel_value.lower(),
                        SuppressionEntry.normalized_value == organization.canonical_domain.lower(),
                    ),
                )
            )
            is not None
        )

    @staticmethod
    def _paused(session: Session, organization: Organization, thread: EmailThread) -> bool:
        if organization.paused or thread.sequence_paused:
            return True
        domain = session.scalar(select(Domain).where(Domain.name == organization.target_domain))
        if domain and domain.paused:
            return True
        return (
            session.scalar(
                select(PauseControl.id)
                .where(
                    PauseControl.scope == "REGION",
                    PauseControl.value == organization.region,
                    PauseControl.paused.is_(True),
                )
                .limit(1)
            )
            is not None
        )

    def _halted_by_health(self, session: Session) -> bool:
        total = (
            session.scalar(
                select(func.count(EmailMessage.id)).where(EmailMessage.direction == "OUTBOUND")
            )
            or 0
        )
        bounced = (
            session.scalar(
                select(func.count(EmailMessage.id)).where(EmailMessage.status == "BOUNCED")
            )
            or 0
        )
        complaints = (
            session.scalar(
                select(func.count(ReplyClassification.id)).where(
                    ReplyClassification.classification == "HOSTILE_OR_SPAM_COMPLAINT"
                )
            )
            or 0
        )
        return bool(
            (total and bounced / total > self.policy.limits.bounce_halt_rate)
            or complaints >= self.policy.limits.complaint_halt_count
        )
