from __future__ import annotations

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Capability,
    Contact,
    Domain,
    Organization,
    OrganizationSource,
    ProspectScore,
)

WEIGHTS = {
    "domain_fit": 0.14,
    "capability_fit": 0.14,
    "demonstrated_problem": 0.07,
    "organization_type": 0.05,
    "technical_stack_compatibility": 0.08,
    "decision_maker_relevance": 0.05,
    "public_current_activity": 0.06,
    "geographic_eligibility": 0.04,
    "expected_budget": 0.02,
    "pilot_feasibility": 0.10,
    "integration_effort": 0.06,
    "response_likelihood": 0.03,
    "strategic_value": 0.06,
    "conflict_reputational_risk": 0.05,
    "regulatory_risk": 0.05,
}


class ProspectScoringService:
    def __init__(self, target_regions: list[str], excluded_regions: list[str]) -> None:
        self.target_regions = set(target_regions)
        self.excluded_regions = set(excluded_regions)

    def score(self, session: Session) -> list[ProspectScore]:
        capabilities = list(session.scalars(select(Capability)))
        domains = {item.name: item for item in session.scalars(select(Domain))}
        results: list[ProspectScore] = []
        for organization in session.scalars(
            select(Organization).where(Organization.verified.is_(True)).order_by(Organization.id)
        ):
            contacts = list(
                session.scalars(
                    select(Contact).where(
                        Contact.organization_id == organization.id,
                        Contact.public_evidence_verified.is_(True),
                    )
                )
            )
            if not contacts:
                continue
            source_refs = [
                source.source_url
                for source in session.scalars(
                    select(OrganizationSource).where(
                        OrganizationSource.organization_id == organization.id
                    )
                )
            ]
            domain = domains.get(organization.target_domain)
            matches = self._matching_capabilities(organization.target_domain, capabilities)
            demonstrated = [
                item for item in matches if item.readiness == "IMPLEMENTED_AND_DEMONSTRATED"
            ]
            regulated = any(
                token in organization.target_domain.lower()
                for token in ("clinical", "medical", "patient", "uav")
            )
            eligible = organization.region not in self.excluded_regions and (
                not self.target_regions or organization.region in self.target_regions
            )
            components: dict[str, float] = {
                "domain_fit": min(10.0, (domain.score / 10 if domain else 4.0)),
                "capability_fit": min(10.0, 4.0 + len(matches) * 0.8),
                "demonstrated_problem": 8.0 if source_refs else 0.0,
                "organization_type": 8.5
                if organization.organization_type
                in {"company", "standards_organization", "open_source_foundation"}
                else 7.0,
                "technical_stack_compatibility": min(
                    10.0, 4.0 + sum(bool(item.protocols) for item in matches)
                ),
                "decision_maker_relevance": 7.5
                if any(contact.role for contact in contacts)
                else 0.0,
                "public_current_activity": 7.0 if source_refs else 0.0,
                "geographic_eligibility": 10.0 if eligible else 0.0,
                "expected_budget": 0.0,
                "pilot_feasibility": 9.0 if demonstrated else (6.5 if matches else 3.0),
                "integration_effort": 8.0 if demonstrated else 5.5,
                "response_likelihood": 6.0
                if any(contact.channel_type == "email" for contact in contacts)
                else 4.0,
                "strategic_value": 9.0
                if organization.prospect_category in {"integration_partner", "research_partner"}
                else 7.5,
                "conflict_reputational_risk": 8.0,
                "regulatory_risk": 4.5 if regulated else 8.0,
            }
            total = round(sum(components[key] * WEIGHTS[key] for key in WEIGHTS) * 10, 2)
            explanation = (
                f"Evidence-backed score {total:.2f}. {len(matches)} mapped capabilities and "
                f"{len(demonstrated)} demonstrated workflows; budget is unknown and contributes zero. "
                f"Geographic eligibility={'yes' if eligible else 'no'}; regulatory risk is "
                f"{'elevated' if regulated else 'standard B2B'} and remains a compliance gate."
            )
            item = session.scalar(
                select(ProspectScore).where(
                    ProspectScore.organization_id == organization.id,
                    ProspectScore.model_version == "deterministic-v1",
                )
            )
            if item is None:
                item = ProspectScore(
                    organization_id=organization.id,
                    total=total,
                    components=components,
                    explanation=explanation,
                    evidence_refs=source_refs + [cap.capability_id for cap in matches],
                )
                session.add(item)
            else:
                item.total = total
                item.components = components
                item.explanation = explanation
                item.evidence_refs = source_refs + [cap.capability_id for cap in matches]
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.PROSPECT_SCORED,
                reason=explanation,
                source="prospect-score:deterministic-v1",
            )
            results.append(item)
        session.flush()
        return sorted(results, key=lambda item: (-item.total, item.organization_id))

    @staticmethod
    def _matching_capabilities(name: str, capabilities: list[Capability]) -> list[Capability]:
        tokens = {
            token.rstrip("s")
            for token in name.lower().replace("-", " ").split()
            if len(token) > 3 and token not in {"and", "with"}
        }
        return [
            item
            for item in capabilities
            if any(token in (item.domain + " " + item.name).lower() for token in tokens)
        ]
