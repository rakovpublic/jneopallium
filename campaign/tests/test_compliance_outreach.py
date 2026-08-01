from __future__ import annotations

import pytest
from sqlalchemy import select

from jneo_campaign.compliance.service import ComplianceService
from jneo_campaign.config import Settings
from jneo_campaign.providers.mock import MockGmailProvider
from jneo_campaign.storage.models import (
    EmailMessage,
    OutreachPermissionEvidence,
    SuppressionEntry,
)
from tests.helpers import seed_compliance_ready


def _live_settings(runner) -> Settings:
    return Settings(
        _env_file=None,
        campaign_mode="LIVE",
        campaign_live_send=True,
        campaign_config_dir=runner.config.settings.campaign_config_dir,
        campaign_database_url=runner.config.settings.campaign_database_url,
        campaign_repository_root=runner.config.settings.campaign_repository_root,
        campaign_report_dir=runner.config.settings.campaign_report_dir,
        campaign_sender_name="Sender Name",
        campaign_sender_email="sender@example.org",
        campaign_postal_address="1 Example Street, Test City",
        campaign_reply_to="sender@example.org",
    )


def test_dry_run_compliance_approves_without_live_identity(runner) -> None:
    with runner.database.session() as session:
        seed_compliance_ready(session)
        decisions = runner.compliance.review(session)
        assert [item.decision for item in decisions] == ["APPROVED_DRY_RUN"]


def test_live_compliance_fails_closed_without_identity(runner) -> None:
    with runner.database.session() as session:
        seed_compliance_ready(session)
        live_settings = Settings(
            _env_file=None,
            campaign_mode="LIVE",
            campaign_live_send=True,
            campaign_config_dir=runner.config.settings.campaign_config_dir,
            campaign_database_url=runner.config.settings.campaign_database_url,
            campaign_repository_root=runner.config.settings.campaign_repository_root,
            campaign_report_dir=runner.config.settings.campaign_report_dir,
        )
        service = ComplianceService(live_settings, runner.config.compliance)
        decisions = service.review(session)
        assert decisions[0].decision == "BLOCKED"
        assert any("sender" in reason.lower() for reason in decisions[0].reasons)


@pytest.mark.parametrize(
    ("region", "basis", "entity_type", "locale", "expected_lawful_basis"),
    [
        (
            "GB",
            "corporate_subscriber",
            "corporate",
            "en",
            "legitimate_interest_b2b_corporate_subscriber",
        ),
        (
            "CA",
            "conspicuous_publication",
            "corporate",
            "en",
            "implied_consent_conspicuous_publication",
        ),
        ("JP", "public_business_address", "corporate", "en", "public_business_address_exception"),
        ("AU", "inferred_consent_publication", "corporate", "en", "inferred_consent_publication"),
        ("EU", "explicit_consent", "corporate", "en", "explicit_consent"),
        ("KR", "explicit_consent", "corporate", "en", "explicit_consent"),
        ("IL", "explicit_consent", "corporate", "en", "explicit_consent"),
        ("UA", "ukraine_opt_out", "corporate", "uk", "ukraine_opt_out_business_outreach"),
    ],
)
def test_region_specific_permission_evidence_can_approve_live_outreach(
    runner, region, basis, entity_type, locale, expected_lawful_basis
) -> None:
    with runner.database.session() as session:
        organization, contact, _asset = seed_compliance_ready(session)
        organization.region = region
        contact.locale = locale
        session.add(
            OutreachPermissionEvidence(
                contact_id=contact.id,
                basis=basis,
                recipient_entity_type=entity_type,
                public_address=True,
                no_solicitation_notice=False,
                relevant_to_role=True,
                evidence_url="https://example.org/contact",
                evidence_excerpt="Reviewed public permission evidence.",
            )
        )

        decisions = ComplianceService(_live_settings(runner), runner.config.compliance).review(
            session
        )

        assert decisions[0].decision == "APPROVED"
        assert decisions[0].lawful_basis == expected_lawful_basis


@pytest.mark.parametrize("region", ["EU", "CA", "JP", "KR", "UA", "IL", "GB", "AU"])
def test_expanded_region_without_required_permission_evidence_needs_manual_review(
    runner, region
) -> None:
    with runner.database.session() as session:
        organization, _contact, _asset = seed_compliance_ready(session)
        organization.region = region

        decisions = ComplianceService(_live_settings(runner), runner.config.compliance).review(
            session
        )

        assert decisions[0].decision == "MANUAL_LEGAL_REVIEW_REQUIRED"


def test_duplicate_initial_send_is_prevented(runner) -> None:
    assert isinstance(runner.providers.gmail, MockGmailProvider)
    with runner.database.session() as session:
        seed_compliance_ready(session)
        runner.compliance.review(session)
        assert len(runner.outreach.prepare(session)) == 1
        assert len(runner.outreach.prepare(session)) == 0
        assert len(runner.outreach.send(session)) == 1
        assert len(runner.outreach.send(session)) == 0
        assert len(runner.providers.gmail.sent) == 1
        assert len(list(session.scalars(select(EmailMessage)))) == 1


def test_unsent_queued_message_refreshes_when_approved_asset_changes(runner) -> None:
    with runner.database.session() as session:
        _organization, _contact, asset = seed_compliance_ready(session)
        runner.compliance.review(session)
        [message] = runner.outreach.prepare(session)

        asset.content = asset.content.replace(
            "Subject: Example Engineering: bounded technical evaluation",
            "Subject: Updated bounded technical evaluation",
        )
        refreshed = runner.outreach.prepare(session)

        assert refreshed == [message]
        assert message.subject == "Updated bounded technical evaluation"
        assert message.provider_message_id is None
        assert message.sent_at is None


def test_queued_message_is_not_reported_as_sent(runner) -> None:
    with runner.database.session() as session:
        seed_compliance_ready(session)
        runner.compliance.review(session)
        runner.outreach.prepare(session)

        metrics = runner.analytics.report(session, runner.config.campaign.campaign_id)

        assert metrics["messages_prepared"] == 1
        assert metrics["messages_sent_or_simulated"] == 0
        assert metrics["real_external_messages_sent"] == 0
        assert metrics["conversion_by_domain"]["Industrial automation"]["sent"] == 0


def test_suppression_is_checked_before_queueing(runner) -> None:
    with runner.database.session() as session:
        _organization, contact, _asset = seed_compliance_ready(session)
        session.add(
            SuppressionEntry(
                scope="CONTACT",
                normalized_value=contact.channel_value,
                reason="unit test",
                source="unit-test",
            )
        )
        runner.compliance.review(session)
        assert runner.outreach.prepare(session) == []
