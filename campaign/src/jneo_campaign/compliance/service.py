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
    OutreachPermissionEvidence,
    SuppressionEntry,
)

TARGETABLE_LIVE_JURISDICTIONS = {"US", "EU", "CA", "JP", "KR", "UA", "IL", "GB", "AU"}
EXPLICIT_CONSENT_JURISDICTIONS = {"EU", "KR", "IL"}
PUBLICATION_EVIDENCE_RULES = {
    "CA": ("conspicuous_publication", "implied_consent_conspicuous_publication"),
    "JP": ("public_business_address", "public_business_address_exception"),
    "AU": ("inferred_consent_publication", "inferred_consent_publication"),
}


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
        regional_basis: str | None = None
        if self.settings.live_writes_enabled:
            regional_basis, regional_review = self._regional_basis(session, organization, contact)
            if regional_review:
                manual.append(regional_review)
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
        basis = regional_basis or "legitimate_interest_b2b_technical_relevance"
        if self.settings.live_writes_enabled:
            return (
                "APPROVED",
                [
                    f"{organization.region} region-specific outreach ruleset passed",
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

    @staticmethod
    def _regional_basis(
        session: Session, organization: Organization, contact: Contact
    ) -> tuple[str | None, str | None]:
        region = organization.region
        if region not in TARGETABLE_LIVE_JURISDICTIONS:
            return None, f"No configured LIVE legal ruleset for region {region or 'UNKNOWN'}"
        if region == "US":
            return "legitimate_interest_b2b_technical_relevance", None

        evidence = session.scalar(
            select(OutreachPermissionEvidence).where(
                OutreachPermissionEvidence.contact_id == contact.id
            )
        )
        if evidence is None:
            return None, f"{region} outreach requires region-specific permission evidence"
        reviewed_at = evidence.reviewed_at
        if reviewed_at.tzinfo is None:
            reviewed_at = reviewed_at.replace(tzinfo=UTC)
        if reviewed_at < datetime.now(UTC) - timedelta(days=90):
            return None, f"{region} permission evidence is older than 90 days"
        if not evidence.evidence_url or not evidence.evidence_excerpt.strip():
            return None, f"{region} permission evidence requires a URL and review excerpt"
        if evidence.basis == "explicit_consent":
            return "explicit_consent", None

        if region in EXPLICIT_CONSENT_JURISDICTIONS:
            return None, f"{region} automated outreach requires explicit consent evidence"
        if region == "GB":
            if (
                evidence.basis == "corporate_subscriber"
                and evidence.recipient_entity_type == "corporate"
                and contact.name is None
            ):
                return "legitimate_interest_b2b_corporate_subscriber", None
            return None, "GB automation requires a verified corporate subscriber and generic inbox"
        if region in PUBLICATION_EVIDENCE_RULES:
            required_basis, lawful_basis = PUBLICATION_EVIDENCE_RULES[region]
            if (
                evidence.basis == required_basis
                and evidence.public_address
                and evidence.no_solicitation_notice is False
                and evidence.relevant_to_role
            ):
                return lawful_basis, None
            return (
                None,
                f"{region} automation requires a conspicuously published relevant business address "
                "with a recorded absence of solicitation restrictions",
            )
        if region == "UA":
            if evidence.basis == "ukraine_opt_out" and contact.locale == "uk":
                return "ukraine_opt_out_business_outreach", None
            return (
                None,
                "UA automation requires Ukrainian-language opt-out evidence or explicit consent",
            )
        return None, f"{region} permission evidence did not match the configured ruleset"
