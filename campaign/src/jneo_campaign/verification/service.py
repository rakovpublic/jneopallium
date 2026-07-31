from __future__ import annotations

from urllib.parse import urlparse

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import Contact, ContactSource, Organization, OrganizationSource


class VerificationService:
    """Verify provenance, never deliverability by guessing or SMTP probing."""

    def verify(self, session: Session) -> dict[str, int]:
        organizations_verified = 0
        contacts_verified = 0
        organizations = list(session.scalars(select(Organization).order_by(Organization.id)))
        for organization in organizations:
            sources = list(
                session.scalars(
                    select(OrganizationSource).where(
                        OrganizationSource.organization_id == organization.id
                    )
                )
            )
            valid_sources = [source for source in sources if self._valid_source(source)]
            if not valid_sources:
                continue
            organization.verified = True
            organizations_verified += 1
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.ORGANIZATION_VERIFIED,
                reason="At least one official public source has URL, retrieval time, type, and excerpt",
                source=valid_sources[0].source_url,
            )
            contacts = list(
                session.scalars(select(Contact).where(Contact.organization_id == organization.id))
            )
            if contacts:
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.CONTACT_DISCOVERED,
                    reason="A professional channel with public provenance was recorded",
                    source="verification:contact-provenance",
                )
            for contact in contacts:
                contact_sources = list(
                    session.scalars(
                        select(ContactSource).where(ContactSource.contact_id == contact.id)
                    )
                )
                if not contact_sources or not all(
                    self._valid_source(source) for source in contact_sources
                ):
                    continue
                if contact.channel_type not in {"email", "contact_form"}:
                    continue
                if contact.channel_type == "email" and "@" not in contact.channel_value:
                    continue
                contact.public_evidence_verified = True
                contacts_verified += 1
            if any(contact.public_evidence_verified for contact in contacts):
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.CONTACT_VERIFIED,
                    reason="Professional contact channel is explicitly published on an official source",
                    source="verification:public-contact-evidence",
                )
        return {
            "organizations_verified": organizations_verified,
            "contacts_verified": contacts_verified,
        }

    @staticmethod
    def _valid_source(source: OrganizationSource | ContactSource) -> bool:
        parsed = urlparse(source.source_url)
        return bool(
            parsed.scheme == "https"
            and parsed.hostname
            and source.source_type
            and source.supporting_excerpt.strip()
            and source.retrieved_at
            and source.source_hash
        )
