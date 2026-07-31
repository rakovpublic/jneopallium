from __future__ import annotations

import hashlib
import re
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.providers.interfaces import GmailProvider, ProviderMessage
from jneo_campaign.security import sanitize_external_content, text_to_safe_html
from jneo_campaign.state_machine.service import CampaignState, InvalidTransition, transition
from jneo_campaign.storage.models import (
    EmailMessage,
    EmailThread,
    FollowUp,
    Organization,
    ReplyClassification,
    SuppressionEntry,
)


@dataclass(frozen=True)
class Classification:
    label: str
    confidence: float
    explanation: str
    deterministic: bool = True
    escalation: bool = False


ESCALATION_TERMS = {
    "pricing": "PRICING_COMMITMENT",
    "contract": "CONTRACTUAL_OR_LEGAL_CONCERN",
    "warranty": "CONTRACTUAL_OR_LEGAL_CONCERN",
    "regulatory": "MEDICAL_OR_REGULATORY_CONCERN",
    "clinical effectiveness": "MEDICAL_OR_REGULATORY_CONCERN",
    "patient": "MEDICAL_OR_REGULATORY_CONCERN",
    "export control": "CONFIDENTIALITY_OR_LEGAL_CONCERN",
    "classified": "CONFIDENTIALITY_OR_LEGAL_CONCERN",
    "investment terms": "INVESTOR_QUESTION",
    "intellectual property": "CONFIDENTIALITY_OR_LEGAL_CONCERN",
    "exclusivity": "CONTRACTUAL_OR_LEGAL_CONCERN",
    "production deployment": "TECHNICAL_QUESTION",
}


class ReplyProcessingService:
    def __init__(self, provider: GmailProvider) -> None:
        self.provider = provider

    def sync(self, session: Session) -> list[ReplyClassification]:
        # The query cannot match unrelated personal mail: only the campaign label or explicitly
        # imported campaign-thread header is considered.
        provider_messages = self.provider.sync("label:JNEOPALLIUM_CAMPAIGN -from:me")
        results: list[ReplyClassification] = []
        for provider_message in provider_messages:
            thread = session.scalar(
                select(EmailThread).where(
                    EmailThread.provider_thread_id == provider_message.thread_id
                )
            )
            if thread is None:
                continue
            message = session.scalar(
                select(EmailMessage).where(
                    EmailMessage.provider_message_id == provider_message.message_id
                )
            )
            if message is None:
                external = sanitize_external_content(provider_message.body_text)
                message = EmailMessage(
                    thread_id=thread.id,
                    provider_message_id=provider_message.message_id,
                    direction="INBOUND",
                    sequence_number=0,
                    subject=provider_message.subject,
                    body_text=external.text,
                    body_html=text_to_safe_html(external.text),
                    status="RECEIVED",
                    sent_at=provider_message.received_at,
                    delivered_at=provider_message.received_at,
                    idempotency_key=hashlib.sha256(
                        f"inbound|{provider_message.message_id}".encode()
                    ).hexdigest(),
                )
                session.add(message)
                session.flush()
            existing = session.scalar(
                select(ReplyClassification).where(ReplyClassification.message_id == message.id)
            )
            if existing:
                results.append(existing)
                continue
            classification = classify_reply(provider_message)
            item = ReplyClassification(
                message_id=message.id,
                classification=classification.label,
                confidence=classification.confidence,
                explanation=classification.explanation,
                deterministic=classification.deterministic,
                escalation_required=classification.escalation,
            )
            session.add(item)
            results.append(item)
            self._apply(session, thread, classification, provider_message)
        session.flush()
        return results

    def _apply(
        self,
        session: Session,
        thread: EmailThread,
        classification: Classification,
        provider_message: ProviderMessage,
    ) -> None:
        organization = session.get(Organization, thread.organization_id)
        if organization is None:
            return
        if classification.label != "BOUNCE":
            try:
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.REPLIED,
                    reason=classification.explanation,
                    source=f"gmail-reply:{provider_message.message_id}",
                )
            except InvalidTransition:
                # Later messages in an already-positive/meeting thread are still persisted and
                # audited by their classification record; they must not rewind the state machine.
                pass
        target = {
            "POSITIVE_INTEREST": CampaignState.POSITIVE_REPLY,
            "REQUEST_FOR_INFORMATION": CampaignState.QUESTION_RECEIVED,
            "TECHNICAL_QUESTION": CampaignState.QUESTION_RECEIVED,
            "MEETING_REQUEST": CampaignState.MEETING_REQUESTED,
            "PROPOSAL_REQUEST": CampaignState.PROPOSAL_REQUESTED,
            "POC_REQUEST": CampaignState.POC_REQUESTED,
            "NOT_INTERESTED": CampaignState.NOT_INTERESTED,
            "UNSUBSCRIBE": CampaignState.DO_NOT_CONTACT,
            "HOSTILE_OR_SPAM_COMPLAINT": CampaignState.DO_NOT_CONTACT,
            "BOUNCE": CampaignState.BOUNCED,
        }.get(classification.label)
        if target:
            try:
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=target,
                    reason=classification.explanation,
                    source=f"reply-classifier:{classification.label}",
                )
            except InvalidTransition:
                pass
        if classification.label in {
            "UNSUBSCRIBE",
            "HOSTILE_OR_SPAM_COMPLAINT",
            "BOUNCE",
            "NOT_INTERESTED",
        }:
            thread.sequence_paused = True
            thread.status = classification.label
            for followup in session.scalars(
                select(FollowUp).where(
                    FollowUp.thread_id == thread.id, FollowUp.status == "SCHEDULED"
                )
            ):
                followup.status = "STOPPED"
        if classification.label == "BOUNCE":
            latest_outbound = session.scalar(
                select(EmailMessage)
                .where(
                    EmailMessage.thread_id == thread.id,
                    EmailMessage.direction == "OUTBOUND",
                )
                .order_by(EmailMessage.sequence_number.desc())
                .limit(1)
            )
            if latest_outbound is not None:
                latest_outbound.status = "BOUNCED"
        if classification.label in {"UNSUBSCRIBE", "HOSTILE_OR_SPAM_COMPLAINT"}:
            self._suppress(
                session, organization, provider_message.from_address, classification.label
            )

    @staticmethod
    def _suppress(session: Session, organization: Organization, address: str, reason: str) -> None:
        for scope, value in (
            ("CONTACT", _bare_email(address)),
            ("ORGANIZATION", organization.canonical_domain.lower()),
        ):
            if not value:
                continue
            existing = session.scalar(
                select(SuppressionEntry).where(
                    SuppressionEntry.scope == scope,
                    SuppressionEntry.normalized_value == value,
                )
            )
            if existing is None:
                session.add(
                    SuppressionEntry(
                        scope=scope,
                        normalized_value=value,
                        reason=reason,
                        source="reply-classifier",
                    )
                )
            else:
                existing.active = True
                existing.reason = reason


def classify_reply(message: ProviderMessage) -> Classification:
    external = sanitize_external_content(message.body_text)
    text = external.text.lower()
    if external.prompt_injection_suspected:
        return Classification(
            "SECURITY_CONCERN",
            0.99,
            "Prompt-injection-like instructions were quarantined as untrusted email content",
            escalation=True,
        )
    if re.search(r"\b(unsubscribe|remove me|opt[ -]?out|do not contact)\b", text):
        return Classification("UNSUBSCRIBE", 0.99, "Explicit opt-out phrase matched")
    if re.search(r"\b(spam|report(ed)? you|complaint)\b", text) and "not spam" not in text:
        return Classification(
            "HOSTILE_OR_SPAM_COMPLAINT", 0.98, "Spam/complaint phrase matched", escalation=True
        )
    if re.search(r"\b(mailbox unavailable|delivery failed|undeliverable|550 5\.)\b", text):
        return Classification("BOUNCE", 0.99, "Delivery-status phrase matched")
    if re.search(r"\b(out of office|automatic reply|away until|on leave)\b", text):
        return Classification("OUT_OF_OFFICE", 0.98, "Out-of-office phrase matched")
    for term, label in ESCALATION_TERMS.items():
        if term in text:
            return Classification(label, 0.95, f"Escalation term matched: {term}", escalation=True)
    if re.search(r"\b(option [123]|schedule|calendar|meet|call|next week|time works)\b", text):
        return Classification("MEETING_REQUEST", 0.94, "Meeting or scheduling phrase matched")
    if re.search(r"\b(proposal|statement of work|sow)\b", text):
        return Classification(
            "PROPOSAL_REQUEST", 0.94, "Proposal request phrase matched", escalation=True
        )
    if re.search(r"\b(proof of concept|poc|pilot)\b", text):
        return Classification("POC_REQUEST", 0.93, "Proof-of-concept request phrase matched")
    if re.search(r"\b(not interested|no thanks|please stop)\b", text):
        return Classification("NOT_INTERESTED", 0.98, "Negative-interest phrase matched")
    if re.search(r"\b(not now|later this year|circle back|revisit)\b", text):
        return Classification("NOT_NOW", 0.93, "Deferred-interest phrase matched")
    if "?" in text and re.search(
        r"\b(api|schema|protocol|latency|model|baseline|demo|integration)\b", text
    ):
        return Classification(
            "TECHNICAL_QUESTION", 0.91, "Question mark plus technical term matched"
        )
    if "?" in text or re.search(r"\b(send|share) (more|details|information)\b", text):
        return Classification(
            "REQUEST_FOR_INFORMATION", 0.88, "Information-request pattern matched"
        )
    if re.search(r"\b(interested|promising|relevant|tell me more|sounds useful)\b", text):
        return Classification("POSITIVE_INTEREST", 0.90, "Positive-interest phrase matched")
    if re.search(r"\b(contact|speak to|forwarded|copying)\b", text):
        return Classification("REFERRAL", 0.82, "Referral phrase matched")
    return Classification(
        "UNRELATED", 0.50, "No high-confidence deterministic rule matched", deterministic=True
    )


def _bare_email(value: str) -> str:
    match = re.search(r"[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}", value, re.I)
    return match.group(0).lower() if match else value.strip().lower()
