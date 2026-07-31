from __future__ import annotations

from sqlalchemy import select

from jneo_campaign.compliance.service import ComplianceService
from jneo_campaign.config import Settings
from jneo_campaign.providers.mock import MockGmailProvider
from jneo_campaign.storage.models import (
    EmailMessage,
    SuppressionEntry,
)
from tests.helpers import seed_compliance_ready


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
