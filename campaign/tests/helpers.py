from __future__ import annotations

import hashlib
from datetime import UTC, datetime

from sqlalchemy.orm import Session

from jneo_campaign.storage.models import (
    Campaign,
    Contact,
    ContactSource,
    GeneratedAsset,
    Organization,
    OrganizationSource,
    WorkflowState,
)


def seed_compliance_ready(session: Session) -> tuple[Organization, Contact, GeneratedAsset]:
    organization = Organization(
        name="Example Engineering",
        canonical_domain="example.org",
        organization_type="company",
        prospect_category="integration_partner",
        target_domain="Industrial automation",
        region="US",
        country="US",
        summary="Official site describes industrial integration engineering.",
        verified=True,
    )
    session.add(organization)
    session.flush()
    digest = hashlib.sha256(b"source").hexdigest()
    session.add(
        OrganizationSource(
            organization_id=organization.id,
            source_url="https://example.org/engineering",
            source_type="official_website",
            supporting_excerpt="Official engineering activity.",
            source_hash=digest,
            retrieved_at=datetime.now(UTC),
        )
    )
    contact = Contact(
        organization_id=organization.id,
        role="Partnership team",
        channel_type="email",
        channel_value="partnerships@example.org",
        professional=True,
        public_evidence_verified=True,
        timezone="America/New_York",
    )
    session.add(contact)
    session.flush()
    session.add(
        ContactSource(
            contact_id=contact.id,
            source_url="https://example.org/contact",
            source_type="official_contact_page",
            supporting_excerpt="Official partnership address.",
            source_hash=digest,
            retrieved_at=datetime.now(UTC),
        )
    )
    content = (
        "Subject: Example Engineering: bounded technical evaluation\n\n"
        "I am writing because the official engineering page describes this activity.\n\n"
        "Jneopallium repository evidence supports a synthetic advisory evaluation.\n\n"
        "Reply unsubscribe to stop contact."
    )
    asset = GeneratedAsset(
        organization_id=organization.id,
        asset_type="introductory_email",
        persona="system_architect",
        format="markdown",
        content=content,
        evidence_refs=["https://example.org/engineering", "domain.industrial"],
        content_hash=hashlib.sha256(content.encode()).hexdigest(),
        validated=True,
    )
    session.add(asset)
    session.add(
        WorkflowState(
            entity_type="prospect",
            entity_id=str(organization.id),
            state="MATERIALS_GENERATED",
        )
    )
    session.flush()
    return organization, contact, asset


def seed_campaign(session: Session) -> Campaign:
    campaign = Campaign(
        campaign_key="dry-run-pilot",
        name="Test",
        audience="integration_partner",
        config_snapshot={},
    )
    session.add(campaign)
    session.flush()
    return campaign
