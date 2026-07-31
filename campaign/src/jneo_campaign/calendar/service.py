from __future__ import annotations

import hashlib
import re
from datetime import UTC, datetime, time, timedelta
from zoneinfo import ZoneInfo

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.config import Settings
from jneo_campaign.providers.interfaces import (
    CalendarProvider,
    CalendarSlot,
    GmailProvider,
    OutboundEmail,
)
from jneo_campaign.security import text_to_safe_html
from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Contact,
    EmailMessage,
    EmailThread,
    Meeting,
    Organization,
    WorkflowState,
)


class MeetingCoordinator:
    def __init__(
        self,
        settings: Settings,
        calendar_provider: CalendarProvider,
        gmail_provider: GmailProvider,
    ) -> None:
        self.settings = settings
        self.calendar_provider = calendar_provider
        self.gmail_provider = gmail_provider

    def propose(self, session: Session) -> list[Meeting]:
        created: list[Meeting] = []
        threads = list(session.scalars(select(EmailThread).order_by(EmailThread.id)))
        for thread in threads:
            workflow = session.scalar(
                select(WorkflowState).where(
                    WorkflowState.entity_type == "prospect",
                    WorkflowState.entity_id == str(thread.organization_id),
                    WorkflowState.state == CampaignState.MEETING_REQUESTED.value,
                )
            )
            if workflow is None:
                continue
            existing = session.scalar(select(Meeting).where(Meeting.thread_id == thread.id))
            if existing:
                continue
            contact = session.get(Contact, thread.contact_id)
            organization = session.get(Organization, thread.organization_id)
            if not contact or not organization or not contact.timezone:
                continue
            slots = self._three_slots(contact.timezone)
            meeting = Meeting(
                thread_id=thread.id,
                timezone=contact.timezone,
                options=[
                    {"number": index, "start": slot.start.isoformat(), "end": slot.end.isoformat()}
                    for index, slot in enumerate(slots, 1)
                ],
                agenda=(
                    f"1. Validate {organization.target_domain} problem evidence; 2. review current "
                    "repository capability and limits; 3. agree or reject a synthetic proof-of-concept; "
                    "4. owners and next decision."
                ),
            )
            session.add(meeting)
            session.flush()
            body = self._options_body(meeting, organization)
            self._send_thread_message(
                session,
                thread,
                contact,
                sequence_number=100,
                subject=f"Re: {thread.subject}",
                body=body,
                key=f"meeting-options|{meeting.id}",
                labels=["JNEOPALLIUM_CAMPAIGN", "JNEOPALLIUM_CAMPAIGN/MEETING"],
            )
            created.append(meeting)
        return created

    def confirm_agreements(self, session: Session) -> list[Meeting]:
        scheduled: list[Meeting] = []
        for meeting in session.scalars(select(Meeting).where(Meeting.status == "OPTIONS_PROPOSED")):
            thread = session.get(EmailThread, meeting.thread_id)
            if thread is None:
                continue
            messages = list(
                session.scalars(
                    select(EmailMessage)
                    .where(
                        EmailMessage.thread_id == thread.id,
                        EmailMessage.direction == "INBOUND",
                    )
                    .order_by(EmailMessage.sent_at.desc())
                )
            )
            selected = None
            for message in messages:
                match = re.search(
                    r"\b(?:agree to|choose|select|take) option\s*([123])\b", message.body_text, re.I
                )
                if match:
                    selected = meeting.options[int(match.group(1)) - 1]
                    break
            if selected is None:
                continue
            start = datetime.fromisoformat(selected["start"])
            end = datetime.fromisoformat(selected["end"])
            busy = self.calendar_provider.free_busy(start, end, "primary")
            if any(slot.start < end and slot.end > start for slot in busy):
                meeting.status = "CONFLICT_RECHECK_REQUIRED"
                continue
            meeting.recipient_agreed = True
            meeting.agreed_start = start
            meeting.agreed_end = end
            organization = session.get(Organization, thread.organization_id)
            contact = session.get(Contact, thread.contact_id)
            if not organization or not contact:
                continue
            key = hashlib.sha256(f"meeting|{thread.id}|{start.isoformat()}".encode()).hexdigest()
            event_id = self.calendar_provider.create_event(
                summary=f"Jneopallium technical fit review — {organization.name}",
                description=meeting.agenda
                + "\n\nRepository: https://github.com/rakovpublic/jneopallium",
                start=start,
                end=end,
                timezone=meeting.timezone,
                attendees=[contact.channel_value],
                idempotency_key=key,
            )
            meeting.provider_event_id = event_id
            meeting.status = "MEETING_SCHEDULED"
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.MEETING_SCHEDULED,
                reason="Recipient explicitly selected a proposed option and free/busy was rechecked",
                source=f"calendar:{event_id}",
            )
            confirmation = (
                f"Confirmed for {start.astimezone(ZoneInfo(meeting.timezone)).isoformat()}.\n\n"
                f"Agenda: {meeting.agenda}\n\n"
                "The event was created only after your explicit option selection."
            )
            self._send_thread_message(
                session,
                thread,
                contact,
                sequence_number=101,
                subject=f"Confirmed: {organization.name} / Jneopallium technical review",
                body=confirmation,
                key=f"meeting-confirmation|{meeting.id}|{event_id}",
                labels=["JNEOPALLIUM_CAMPAIGN", "JNEOPALLIUM_CAMPAIGN/MEETING"],
            )
            scheduled.append(meeting)
        return scheduled

    def _three_slots(self, recipient_timezone: str) -> list[CalendarSlot]:
        recipient_zone = ZoneInfo(recipient_timezone)
        sender_zone = ZoneInfo(self.settings.campaign_default_timezone)
        now = datetime.now(UTC)
        start_search = now + timedelta(days=1)
        end_search = now + timedelta(days=15)
        busy = self.calendar_provider.free_busy(start_search, end_search, "primary")
        candidates: list[CalendarSlot] = []
        local_date = start_search.astimezone(recipient_zone).date()
        while len(candidates) < 3 and local_date <= end_search.astimezone(recipient_zone).date():
            if local_date.weekday() < 5:
                for hour in (9, 10, 11, 14, 15):
                    start_local = datetime.combine(local_date, time(hour=hour), recipient_zone)
                    start_utc = start_local.astimezone(UTC)
                    end_utc = start_utc + timedelta(minutes=30)
                    sender_local = start_utc.astimezone(sender_zone)
                    if not (8 <= sender_local.hour < 19):
                        continue
                    if any(item.start < end_utc and item.end > start_utc for item in busy):
                        continue
                    candidates.append(CalendarSlot(start_utc, end_utc))
                    if len(candidates) == 3:
                        break
            local_date += timedelta(days=1)
        if len(candidates) < 3:
            raise RuntimeError("Could not find three mutually business-hour meeting options")
        return candidates

    @staticmethod
    def _options_body(meeting: Meeting, organization: Organization) -> str:
        zone = ZoneInfo(meeting.timezone)
        lines = [
            f"Thanks for the interest. Here are three 30-minute options for {organization.name} "
            f"in {meeting.timezone}:",
            "",
        ]
        for option in meeting.options:
            start = datetime.fromisoformat(option["start"]).astimezone(zone)
            lines.append(f"Option {option['number']}: {start.strftime('%A %Y-%m-%d %H:%M %Z')}")
        lines.extend(
            [
                "",
                "Please reply ‘I agree to option 1’ (or 2/3). Availability will be rechecked before "
                "an event is created.",
                "",
                f"Proposed agenda: {meeting.agenda}",
            ]
        )
        return "\n".join(lines)

    def _send_thread_message(
        self,
        session: Session,
        thread: EmailThread,
        contact: Contact,
        *,
        sequence_number: int,
        subject: str,
        body: str,
        key: str,
        labels: list[str],
    ) -> EmailMessage:
        idempotency_key = hashlib.sha256(key.encode()).hexdigest()
        item = session.scalar(
            select(EmailMessage).where(EmailMessage.idempotency_key == idempotency_key)
        )
        if item:
            return item
        provider_message_id, provider_thread_id = self.gmail_provider.send(
            OutboundEmail(
                to=contact.channel_value,
                subject=subject,
                body_text=body,
                body_html=text_to_safe_html(body),
                reply_to=self.settings.campaign_reply_to or "dry-run@example.invalid",
                idempotency_key=idempotency_key,
                thread_id=thread.provider_thread_id,
            ),
            labels,
        )
        item = EmailMessage(
            thread_id=thread.id,
            provider_message_id=provider_message_id,
            direction="OUTBOUND",
            sequence_number=sequence_number,
            subject=subject,
            body_text=body,
            body_html=text_to_safe_html(body),
            status="SENT" if self.settings.live_writes_enabled else "MOCK_SENT",
            sent_at=datetime.now(UTC),
            idempotency_key=idempotency_key,
        )
        session.add(item)
        thread.provider_thread_id = provider_thread_id
        return item
