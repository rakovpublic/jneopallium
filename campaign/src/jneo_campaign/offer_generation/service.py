from __future__ import annotations

from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import Capability, Offer, Organization, ProspectScore

OFFER_RULES = [
    ({"ad fraud", "advertising"}, "ad-fraud-evaluation"),
    ({"industrial", "maintenance", "digital twin", "process"}, "industrial-loop-guardian"),
    ({"clinical", "medical", "health"}, "health-it-advisory"),
    ({"cyber", "security"}, "security-triage"),
    ({"robot", "uav", "swarm"}, "robotics-guard"),
    ({"nengo", "neuroscience"}, "nengo-interop"),
]


class OfferGenerationService:
    def __init__(self, raw_config: dict[str, Any], minimum_score: float) -> None:
        self.offers = raw_config.get("offers", {})
        self.minimum_score = minimum_score

    def generate(self, session: Session) -> list[Offer]:
        scores = list(
            session.scalars(
                select(ProspectScore)
                .where(ProspectScore.total >= self.minimum_score)
                .order_by(ProspectScore.total.desc(), ProspectScore.organization_id)
            )
        )
        generated: list[Offer] = []
        for score in scores:
            organization = session.get(Organization, score.organization_id)
            if organization is None or organization.paused:
                continue
            key = self._select_key(organization.target_domain)
            config = self.offers.get(key)
            if not config:
                continue
            capabilities = self._capabilities(session, config.get("capability_domains", []))
            evidence = list(
                dict.fromkeys(score.evidence_refs + [item.capability_id for item in capabilities])
            )
            implemented = [
                item
                for item in capabilities
                if item.readiness
                in {"IMPLEMENTED_AND_DEMONSTRATED", "IMPLEMENTED_NOT_DEMONSTRATED"}
            ]
            claim = (
                implemented[0].allowed_claims[0]
                if implemented
                else "A proposed proof of concept would test this fit; it is not current functionality."
            )
            proposition = (
                f"{claim} For {organization.name}, the bounded proposition is: {config['problem']} "
                f"Proposed next step: {config['next_step']} [evidence: {', '.join(evidence[:8])}]"
            )
            item = session.scalar(
                select(Offer).where(
                    Offer.organization_id == organization.id,
                    Offer.offer_type == config["type"],
                )
            )
            if item is None:
                item = Offer(
                    organization_id=organization.id,
                    offer_type=config["type"],
                    problem=config["problem"],
                    proposition=proposition,
                    capability_ids=[cap.capability_id for cap in capabilities],
                    evidence_refs=evidence,
                    limitations=self._limitations(capabilities),
                )
                session.add(item)
            else:
                item.problem = config["problem"]
                item.proposition = proposition
                item.capability_ids = [cap.capability_id for cap in capabilities]
                item.evidence_refs = evidence
                item.limitations = self._limitations(capabilities)
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.OFFER_SELECTED,
                reason=f"Selected {key} from verified domain and capability evidence",
                source=f"offer-rule:{key}",
            )
            generated.append(item)
        session.flush()
        return generated

    @staticmethod
    def _select_key(domain: str) -> str:
        lowered = domain.lower()
        for terms, key in OFFER_RULES:
            if any(term in lowered for term in terms):
                return key
        return "nengo-interop" if "nengo" in lowered else "industrial-loop-guardian"

    @staticmethod
    def _capabilities(session: Session, domains: list[str]) -> list[Capability]:
        lowered = [item.lower() for item in domains]
        return [
            item
            for item in session.scalars(select(Capability))
            if any(term in (item.domain + " " + item.name).lower() for term in lowered)
        ]

    @staticmethod
    def _limitations(capabilities: list[Capability]) -> list[str]:
        limits = [limit for item in capabilities for limit in item.limitations]
        limits.extend(
            [
                "Repository demonstrations are not evidence of customer deployment or business outcome",
                "Customer-specific integration, security review, and acceptance criteria are required",
            ]
        )
        return list(dict.fromkeys(limits))
