from __future__ import annotations

import hashlib

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.providers.interfaces import SearchProvider
from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Contact,
    ContactSource,
    Organization,
    OrganizationSource,
)


class ProspectDiscoveryService:
    def __init__(self, provider: SearchProvider) -> None:
        self.provider = provider

    def discover(self, session: Session, domains: list[str], limit: int = 50) -> dict[str, int]:
        created_organizations = 0
        created_contacts = 0
        for fact in self.provider.discover(domains, limit):
            organization = session.scalar(
                select(Organization).where(Organization.canonical_domain == fact.canonical_domain)
            )
            if organization is None:
                organization = Organization(
                    name=fact.organization_name,
                    canonical_domain=fact.canonical_domain.lower(),
                    organization_type=fact.organization_type,
                    prospect_category=fact.prospect_category,
                    target_domain=fact.target_domain,
                    region=fact.region,
                    country=fact.country,
                    summary=fact.summary,
                )
                session.add(organization)
                session.flush()
                created_organizations += 1
                transition(
                    session,
                    entity_type="prospect",
                    entity_id=organization.id,
                    to_state=CampaignState.ORGANIZATION_DISCOVERED,
                    reason="Organization found through an allowlisted public source type",
                    source=f"search-provider:{self.provider.name}",
                )
            else:
                organization.name = fact.organization_name
                organization.summary = fact.summary
                organization.target_domain = fact.target_domain
                organization.region = fact.region
                organization.country = fact.country
            self._organization_source(session, organization.id, fact)
            if fact.contact_channel_value and fact.contact_source_url:
                contact = session.scalar(
                    select(Contact).where(
                        Contact.organization_id == organization.id,
                        Contact.channel_value == fact.contact_channel_value,
                    )
                )
                if contact is None:
                    contact = Contact(
                        organization_id=organization.id,
                        role=fact.contact_role or "Official professional contact channel",
                        channel_type=fact.contact_channel_type or "contact_form",
                        channel_value=fact.contact_channel_value,
                        professional=True,
                        timezone=fact.contact_timezone,
                    )
                    session.add(contact)
                    session.flush()
                    created_contacts += 1
                elif fact.contact_timezone:
                    contact.timezone = fact.contact_timezone
                self._contact_source(session, contact.id, fact)
        return {
            "organizations_created": created_organizations,
            "contacts_created": created_contacts,
            "organizations_total": len(
                list(session.scalars(select(Organization).order_by(Organization.id)))
            ),
            "contacts_total": len(list(session.scalars(select(Contact).order_by(Contact.id)))),
        }

    @staticmethod
    def _organization_source(session: Session, organization_id: int, fact: object) -> None:
        existing = session.scalar(
            select(OrganizationSource).where(
                OrganizationSource.organization_id == organization_id,
                OrganizationSource.source_url == fact.source_url,
            )
        )
        digest = hashlib.sha256(f"{fact.source_url}|{fact.supporting_excerpt}".encode()).hexdigest()
        if existing is None:
            session.add(
                OrganizationSource(
                    organization_id=organization_id,
                    source_url=fact.source_url,
                    source_type=fact.source_type,
                    supporting_excerpt=fact.supporting_excerpt,
                    source_hash=digest,
                )
            )
        else:
            existing.supporting_excerpt = fact.supporting_excerpt
            existing.source_hash = digest

    @staticmethod
    def _contact_source(session: Session, contact_id: int, fact: object) -> None:
        existing = session.scalar(
            select(ContactSource).where(
                ContactSource.contact_id == contact_id,
                ContactSource.source_url == fact.contact_source_url,
            )
        )
        digest = hashlib.sha256(
            f"{fact.contact_source_url}|{fact.contact_supporting_excerpt}".encode()
        ).hexdigest()
        if existing is None:
            session.add(
                ContactSource(
                    contact_id=contact_id,
                    source_url=fact.contact_source_url,
                    source_type="official_contact_page",
                    supporting_excerpt=fact.contact_supporting_excerpt
                    or "Official contact channel",
                    source_hash=digest,
                )
            )
        else:
            existing.supporting_excerpt = (
                fact.contact_supporting_excerpt or "Official contact channel"
            )
            existing.source_hash = digest
