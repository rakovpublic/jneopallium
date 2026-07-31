from __future__ import annotations

from datetime import UTC, datetime, timedelta

from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from jneo_campaign.config import CompliancePolicy, Settings
from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    ComplianceDecision,
    Contact,
    ContactSource,
    GeneratedAsset,
    Organization,
    OrganizationSource,
    SuppressionEntry,
)

SUPPORTED_LIVE_JURISDICTIONS = {"US"}


class ComplianceService:
    def __init__(self, settings: Settings, policy: CompliancePolicy) -> None:
        self.settings = settings
        self.policy = policy

    def review(self, session: Session) -> list[ComplianceDecision]:
        decisions: list[ComplianceDecision] = []
        for asset in session.scalars(
            select(GeneratedAsset).where(
                GeneratedAsset.asset_type == "introductory_email",
                GeneratedAsset.format == "markdown",
                GeneratedAsset.validated.is_(True),
            )
        ):
            organization = session.get(Organization, asset.organization_id)
            if organization is None:
                continue
            contact = session.scalar(
                select(Contact).where(
                    Contact.organization_id == organization.id,
                    Contact.channel_type == "email",
                    Contact.public_evidence_verified.is_(True),
                )
            )
            if contact is None or asset.persona != self._primary_persona(
                organization.target_domain
            ):
                continue
            decision, reasons, basis, source_refs = self._evaluate(
                session, organization, contact, asset
            )
            item = session.scalar(
                select(ComplianceDecision).where(
                    ComplianceDecision.contact_id == contact.id,
                    ComplianceDecision.asset_id == asset.id,
                    ComplianceDecision.policy_version == "policy-v1",
                )
            )
            if item is None:
                item = ComplianceDecision(
                    organization_id=organization.id,
                    contact_id=contact.id,
                    asset_id=asset.id,
                    decision=decision,
                    reasons=reasons,
                    lawful_basis=basis,
                    source_refs=source_refs,
                )
                session.add(item)
            else:
                item.decision = decision
                item.reasons = reasons
                item.lawful_basis = basis
                item.source_refs = source_refs
            if decision in {"APPROVED", "APPROVED_DRY_RUN"}:
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.COMPLIANCE_APPROVED,
                    reason="; ".join(reasons),
                    source="compliance:policy-v1",
                )
            elif decision == "DO_NOT_CONTACT":
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.DO_NOT_CONTACT,
                    reason="; ".join(reasons),
                    source="compliance:suppression",
                )
            decisions.append(item)
        session.flush()
        return decisions

    def _evaluate(
        self,
        session: Session,
        organization: Organization,
        contact: Contact,
        asset: GeneratedAsset,
    ) -> tuple[str, list[str], str | None, list[str]]:
        blocking: list[str] = []
        manual: list[str] = []
        sources = list(
            session.scalars(
                select(OrganizationSource).where(
                    OrganizationSource.organization_id == organization.id
                )
            )
        )
        contact_sources = list(
            session.scalars(select(ContactSource).where(ContactSource.contact_id == contact.id))
        )
        source_refs = [item.source_url for item in sources + contact_sources]
        suppressed = session.scalar(
            select(SuppressionEntry).where(
                SuppressionEntry.active.is_(True),
                or_(
                    SuppressionEntry.normalized_value == contact.channel_value.lower(),
                    SuppressionEntry.normalized_value == organization.canonical_domain.lower(),
                ),
            )
        )
        if suppressed:
            return "DO_NOT_CONTACT", [f"Suppression entry: {suppressed.reason}"], None, source_refs
        if organization.organization_type not in self.policy.allowed_organization_types:
            blocking.append("Organization type is not allowlisted for professional outreach")
        if not organization.verified or not contact.public_evidence_verified:
            blocking.append("Organization/contact provenance is not verified")
        if not sources or not contact_sources:
            blocking.append("Source URL, retrieval time, type, and excerpt are required")
        if not contact.professional:
            blocking.append("Consumer or non-professional contact is forbidden")
        domain = contact.channel_value.rsplit("@", 1)[-1].lower()
        if domain in self.policy.personal_email_domains:
            blocking.append("Personal mailbox domain requires manual consent evidence")
        if organization.region in set(self.policy.geographic_exclusions):
            blocking.append("Geographic region is excluded")
        if not asset.evidence_refs:
            blocking.append("Generated claim has no evidence references")
        lowered = asset.content.lower()
        for pattern in self.policy.prohibited_claim_patterns:
            if pattern.lower() in lowered:
                blocking.append(f"Prohibited or unsupported claim pattern: {pattern}")
        for required in ("unsubscribe", "jneopallium", "because"):
            if required not in lowered:
                blocking.append(f"Required outreach element is missing: {required}")
        freshest = max((source.retrieved_at for source in sources + contact_sources), default=None)
        if freshest and freshest.tzinfo is None:
            freshest = freshest.replace(tzinfo=UTC)
        if self.settings.live_writes_enabled:
            if organization.region not in SUPPORTED_LIVE_JURISDICTIONS:
                manual.append(
                    f"No approved LIVE legal ruleset for region {organization.region or 'UNKNOWN'}"
                )
            if not contact.timezone:
                manual.append("Recipient business timezone is not verified")
            if not self.settings.campaign_sender_name or not self.settings.campaign_sender_email:
                blocking.append("Truthful sender name and email are not configured")
            if not self.settings.campaign_reply_to:
                blocking.append("Valid reply address is not configured")
            if self.policy.require_postal_address and not self.settings.campaign_postal_address:
                blocking.append("Postal or organizational address is not configured")
            if freshest is None or freshest < datetime.now(UTC) - timedelta(days=90):
                manual.append("Contact-source evidence is older than 90 days")
        if blocking:
            return "BLOCKED", blocking + manual, None, source_refs
        if manual:
            return "MANUAL_LEGAL_REVIEW_REQUIRED", manual, None, source_refs
        basis = "legitimate_interest_b2b_technical_relevance"
        if self.settings.live_writes_enabled:
            return (
                "APPROVED",
                [
                    "US professional B2B ruleset passed",
                    "Suppression checked immediately before queueing",
                ],
                basis,
                source_refs,
            )
        return (
            "APPROVED_DRY_RUN",
            ["All evidence/content checks passed", "External writes remain disabled"],
            basis,
            source_refs,
        )

    @staticmethod
    def _primary_persona(domain: str) -> str:
        value = domain.lower()
        if "ad fraud" in value:
            return "adtech_fraud_lead"
        if any(term in value for term in ("clinical", "medical", "health")):
            return "clinical_informatics_lead"
        if "cyber" in value or "security" in value:
            return "cybersecurity_lead"
        if any(term in value for term in ("industrial", "robot", "uav")):
            return "system_architect"
        return "research_director"
