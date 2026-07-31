from __future__ import annotations

from sqlalchemy import select

from jneo_campaign.storage.models import Capability, Contact, Domain, Organization


def test_repository_inventory_has_complete_claim_fields(runner) -> None:
    with runner.database.session() as session:
        result = runner._inventory(session)
        assert result["capabilities"] >= 40
        capabilities = list(session.scalars(select(Capability)))
        assert all(item.readiness for item in capabilities)
        assert all(item.allowed_claims for item in capabilities)
        assert all(item.prohibited_claims for item in capabilities)
        assert any(item.capability_id == "demo.adfraud" for item in capabilities)
        assert any(item.capability_id == "bridge.fhir" for item in capabilities)


def test_domains_and_prospects_meet_acceptance_counts(runner) -> None:
    with runner.database.session() as session:
        runner._inventory(session)
        research = runner._domain_research(session)
        discovered = runner._prospect_discovery(session)
        verified = runner.verification.verify(session)
        assert research["domains_scored"] == 60
        assert discovered["organizations_total"] >= 25
        assert verified["organizations_verified"] >= 25
        assert verified["contacts_verified"] >= 10
        assert len(list(session.scalars(select(Domain)))) == 60
        assert len(list(session.scalars(select(Organization)))) >= 25
        assert len(list(session.scalars(select(Contact)))) >= 10
