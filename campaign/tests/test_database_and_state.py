from __future__ import annotations

import pytest
from sqlalchemy import inspect, select

from jneo_campaign.state_machine.service import (
    CampaignState,
    InvalidTransition,
    transition,
)
from jneo_campaign.storage.models import AuditEvent, WorkflowState


def test_database_contains_required_entities(runner) -> None:
    names = set(inspect(runner.database.engine).get_table_names())
    expected = {
        "domains",
        "domain_research_findings",
        "capabilities",
        "capability_evidence",
        "organizations",
        "organization_sources",
        "contacts",
        "contact_sources",
        "prospect_scores",
        "offers",
        "demos",
        "demo_plans",
        "generated_assets",
        "campaigns",
        "campaign_variants",
        "email_threads",
        "email_messages",
        "reply_classifications",
        "followups",
        "meetings",
        "compliance_decisions",
        "suppression_entries",
        "experiments",
        "metric_snapshots",
        "audit_events",
    }
    assert expected <= names


def test_state_transition_is_repeat_safe_and_audited(runner) -> None:
    with runner.database.session() as session:
        first = transition(
            session,
            entity_type="domain",
            entity_id="x",
            to_state=CampaignState.DOMAIN_DISCOVERED,
            reason="test",
            source="unit-test",
        )
        second = transition(
            session,
            entity_type="domain",
            entity_id="x",
            to_state=CampaignState.DOMAIN_DISCOVERED,
            reason="test",
            source="unit-test",
        )
        assert first.id == second.id
        assert len(list(session.scalars(select(AuditEvent)))) == 1
        transition(
            session,
            entity_type="domain",
            entity_id="x",
            to_state=CampaignState.DOMAIN_RESEARCHED,
            reason="researched",
            source="unit-test",
        )
        assert session.scalar(select(WorkflowState)).state == "DOMAIN_RESEARCHED"
        assert len(list(session.scalars(select(AuditEvent)))) == 2


def test_invalid_transition_is_rejected(runner) -> None:
    with runner.database.session() as session:
        transition(
            session,
            entity_type="prospect",
            entity_id="x",
            to_state=CampaignState.ORGANIZATION_DISCOVERED,
            reason="test",
            source="unit-test",
        )
        with pytest.raises(InvalidTransition):
            transition(
                session,
                entity_type="prospect",
                entity_id="x",
                to_state=CampaignState.MEETING_SCHEDULED,
                reason="skip",
                source="unit-test",
            )


def test_replaying_completed_pre_outreach_stage_does_not_regress(runner) -> None:
    with runner.database.session() as session:
        current = transition(
            session,
            entity_type="prospect",
            entity_id="replay-safe",
            to_state=CampaignState.QUEUED,
            reason="already queued",
            source="unit-test",
        )

        replayed = transition(
            session,
            entity_type="prospect",
            entity_id="replay-safe",
            to_state=CampaignState.MATERIALS_GENERATED,
            reason="asset refresh",
            source="unit-test",
        )

        assert replayed.id == current.id
        assert replayed.state == CampaignState.QUEUED
        assert replayed.version == current.version


def test_replaying_domain_discovery_does_not_regress_scored_domain(runner) -> None:
    with runner.database.session() as session:
        current = transition(
            session,
            entity_type="domain",
            entity_id="industrial",
            to_state=CampaignState.DOMAIN_SCORED,
            reason="already scored",
            source="unit-test",
        )

        replayed = transition(
            session,
            entity_type="domain",
            entity_id="industrial",
            to_state=CampaignState.DOMAIN_DISCOVERED,
            reason="scheduled refresh",
            source="unit-test",
        )

        assert replayed.id == current.id
        assert replayed.state == CampaignState.DOMAIN_SCORED
        assert replayed.version == current.version
