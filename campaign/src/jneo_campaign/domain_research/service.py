from __future__ import annotations

import json
import re
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import Capability, Domain, DomainResearchFinding

INITIAL = [
    "Industrial automation",
    "Industrial digital twins",
    "Predictive maintenance",
    "Process supervision and safety gating",
    "UAV autonomy",
    "Multi-UAV and swarm coordination",
    "Mobile and industrial robotics",
    "Cybersecurity triage",
    "Adaptive cyber defence",
    "Ad fraud and invalid-traffic detection",
    "Clinical decision support",
    "Medical workflow supervision",
    "Medical imaging context integration",
    "Adaptive tutoring",
    "Educational platform integration",
    "BCI and neurotechnology",
    "Autonomous AI safety",
    "Cognitive-agent research",
    "LLM advisory and orchestration",
    "Nengo and computational-neuroscience interoperability",
]
ADJACENT = [
    "Smart-grid supervision and energy systems",
    "Renewable-energy asset monitoring",
    "Building automation and smart infrastructure",
    "Warehouse robotics",
    "Logistics and fleet coordination",
    "Agricultural robotics",
    "Environmental monitoring",
    "Search-and-rescue robotics",
    "Telecom and edge-network orchestration",
    "AIOps and observability",
    "Distributed event-processing supervision",
    "Automotive and connected-vehicle systems",
    "Rail and transport infrastructure",
    "Aerospace and space-system simulation",
    "Pharmaceutical manufacturing",
    "Bioprocess digital twins",
    "Medical-device monitoring",
    "Remote patient-monitoring research",
    "Hospital operations and resource optimization",
    "Public-safety communications",
    "Semiconductor and electronics manufacturing",
    "Mining automation",
    "Oil, gas and chemical-process monitoring",
    "Water and wastewater infrastructure",
    "Supply-chain anomaly detection",
]
EXPLORATORY = [
    "Payment fraud",
    "Financial transaction anomaly detection",
    "Insurance claims anomaly detection",
    "Marketplace and affiliate fraud",
    "Account-takeover behaviour analysis",
    "Bot and abuse detection",
    "Content-integrity and brand-safety workflows",
    "Human-machine affect modelling",
    "Game-agent research",
    "Assistive technology",
    "Wearable sensor fusion",
    "Human-performance monitoring",
    "Scientific simulation orchestration",
    "Multi-agent enterprise workflows",
    "Safety middleware for external AI agents",
]

KEYWORDS = {
    "industrial": {
        "industrial",
        "maintenance",
        "process",
        "manufacturing",
        "building",
        "mining",
        "water",
        "bioprocess",
        "energy",
        "grid",
        "digital twin",
    },
    "robotics": {
        "robot",
        "uav",
        "swarm",
        "vehicle",
        "aerospace",
        "search-and-rescue",
        "agricultural",
    },
    "cybersecurity": {"cyber", "security", "account-takeover", "bot", "abuse"},
    "ad-fraud": {"ad fraud", "advertising", "affiliate fraud", "brand-safety"},
    "clinical": {"clinical", "medical", "hospital", "patient", "pharmaceutical", "health"},
    "education": {"tutoring", "educational"},
    "AI": {"ai", "agent", "llm", "cognitive", "enterprise workflows"},
    "neuroscience": {"bci", "neuro", "affect", "wearable", "human-performance", "nengo"},
    "observability": {"observability", "event-processing", "telecom", "supply-chain anomaly"},
}

HIGH_BUYER_CLARITY = {"industrial", "cybersecurity", "ad-fraud", "clinical", "observability"}


def slugify(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")


class DomainResearchService:
    def __init__(
        self, raw_config: dict[str, Any], report_dir: Path, repository_docs_dir: Path
    ) -> None:
        self.config = raw_config
        self.report_dir = report_dir.resolve()
        self.repository_docs_dir = repository_docs_dir.resolve()

    def research(self, session: Session) -> list[dict[str, Any]]:
        capabilities = list(session.scalars(select(Capability)))
        results: list[dict[str, Any]] = []
        for category, names in (
            ("existing_or_near", INITIAL),
            ("strong_adjacent", ADJACENT),
            ("exploratory", EXPLORATORY),
        ):
            for name in names:
                item = self._upsert_domain(session, name, category)
                transition(
                    session,
                    entity_type="domain",
                    entity_id=item.id,
                    to_state=CampaignState.DOMAIN_RESEARCHED,
                    initial_state=CampaignState.DOMAIN_DISCOVERED,
                    reason="Recurring domain evidence audit completed",
                    source="domain-research:deterministic-v1",
                )
                self._store_findings(session, item)
                components, evidence_ids = self._score_components(name, category, capabilities)
                weights = self.config["weights"]
                score = sum(components[key] * float(weights[key]) for key in weights) * 10
                score -= {"existing_or_near": 0.0, "strong_adjacent": 6.0, "exploratory": 12.0}[
                    category
                ]
                score -= components.pop("consequence_penalty", 0.0)
                score = round(max(0.0, score), 2)
                allocation = self._allocation(score)
                item.score = score
                item.score_components = components
                item.allocation = allocation
                item.score_explanation = (
                    f"{allocation}: repository match {components['capability_match']}/10, "
                    f"demo {components['working_demo']}/10, protocol fit {components['protocol_fit']}/10; "
                    "risk and burden components are scored as opportunity (higher is safer/easier)."
                )
                transition(
                    session,
                    entity_type="domain",
                    entity_id=item.id,
                    to_state=CampaignState.DOMAIN_SCORED,
                    reason=item.score_explanation,
                    source="domain-score:configurable-weighted-v1",
                )
                results.append(
                    {
                        "rank": 0,
                        "slug": item.slug,
                        "name": name,
                        "category": category,
                        "score": score,
                        "allocation": allocation,
                        "components": components,
                        "repository_evidence": evidence_ids,
                        "external_sources": self.config.get("evidence_sources", {}).get(name, []),
                        "explanation": item.score_explanation,
                    }
                )
        results.sort(key=lambda row: (-row["score"], row["name"]))
        for index, row in enumerate(results, 1):
            row["rank"] = index
        self._write_reports(results)
        return results

    def _upsert_domain(self, session: Session, name: str, category: str) -> Domain:
        slug = slugify(name)
        item = session.scalar(select(Domain).where(Domain.slug == slug))
        if item is None:
            item = Domain(slug=slug, name=name, category=category)
            session.add(item)
            session.flush()
            transition(
                session,
                entity_type="domain",
                entity_id=item.id,
                to_state=CampaignState.DOMAIN_DISCOVERED,
                reason="Domain is part of the initial universe or recurring discovery feed",
                source="domain-universe:v1",
            )
        else:
            item.name = name
            item.category = category
        return item

    def _store_findings(self, session: Session, domain: Domain) -> None:
        for source in self.config.get("evidence_sources", {}).get(domain.name, []):
            fact = source["excerpt"]
            existing = session.scalar(
                select(DomainResearchFinding).where(
                    DomainResearchFinding.domain_id == domain.id,
                    DomainResearchFinding.fact == fact,
                    DomainResearchFinding.source_url == source["url"],
                )
            )
            if existing is None:
                session.add(
                    DomainResearchFinding(
                        domain_id=domain.id,
                        fact=fact,
                        source_url=source["url"],
                        source_type=source["source_type"],
                        supporting_excerpt=fact,
                    )
                )

    def _score_components(
        self, name: str, category: str, capabilities: list[Capability]
    ) -> tuple[dict[str, float], list[str]]:
        lowered = name.lower()
        matching_groups = [
            group for group, words in KEYWORDS.items() if any(word in lowered for word in words)
        ]
        evidence = [
            item
            for item in capabilities
            if any(
                group.lower() in (item.domain + " " + item.name).lower()
                for group in matching_groups
            )
            or any(
                word in (item.domain + " " + item.name).lower()
                for word in lowered.split()
                if len(word) > 5
            )
        ]
        demonstrated = [
            item for item in evidence if item.readiness == "IMPLEMENTED_AND_DEMONSTRATED"
        ]
        category_base = {"existing_or_near": 8.0, "strong_adjacent": 6.0, "exploratory": 4.0}[
            category
        ]
        capability_match = min(10.0, category_base + min(2.0, len(evidence) * 0.25))
        working_demo = 10.0 if demonstrated else 0.0
        repo_evidence = min(10.0, len(evidence) * 0.8 + len(demonstrated) * 0.8)
        buyer_clear = 8.5 if any(group in HIGH_BUYER_CLARITY for group in matching_groups) else 6.0
        high_risk = any(
            token in lowered
            for token in (
                "clinical",
                "medical",
                "patient",
                "uav",
                "public-safety",
                "pharmaceutical",
                "bioprocess",
            )
        )
        very_high_risk = any(
            token in lowered
            for token in (
                "clinical decision",
                "patient",
                "medical-device",
                "pharmaceutical",
                "bioprocess",
            )
        )
        regulation = 3.5 if very_high_risk else (5.5 if high_risk else 8.0)
        safety = 4.5 if high_risk else 8.0
        protocol_fit = min(10.0, sum(1 for item in evidence if item.protocols) * 2.0)
        components = {
            "capability_match": capability_match,
            "working_demo": working_demo,
            "repository_evidence": repo_evidence,
            "customer_pain": 8.0 if category != "exploratory" else 6.0,
            "identifiable_buyers": buyer_clear,
            "prospect_accessibility": 8.0 if name in self.config.get("active_domains", []) else 6.0,
            "poc_feasibility": 8.5 if demonstrated else (6.5 if evidence else 4.0),
            "sales_cycle": 6.5 if not high_risk else 4.0,
            "regulatory_burden": regulation,
            "safety_risk": safety,
            "competitive_intensity": 5.5,
            "recurring_value": 8.0
            if any(group in HIGH_BUYER_CLARITY for group in matching_groups)
            else 6.0,
            "strategic_value": 8.5 if evidence else 5.0,
            "feedback_likelihood": 8.0 if demonstrated else 6.0,
            "investor_relevance": 7.5 if category != "exploratory" else 5.5,
            "protocol_fit": protocol_fit,
            "consequence_penalty": 10.0 if very_high_risk else (6.0 if high_risk else 0.0),
        }
        return (
            {key: round(value, 2) for key, value in components.items()},
            [item.capability_id for item in evidence],
        )

    def _allocation(self, score: float) -> str:
        thresholds = self.config["thresholds"]
        if score >= thresholds["PURSUE_NOW"]:
            return "PURSUE_NOW"
        if score >= thresholds["PILOT"]:
            return "PILOT"
        if score >= thresholds["RESEARCH_MORE"]:
            return "RESEARCH_MORE"
        if score >= thresholds["DEFER"]:
            return "DEFER"
        return "DO_NOT_PURSUE"

    def _write_reports(self, results: list[dict[str, Any]]) -> None:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        payload = {
            "schema_version": "1.0",
            "generated_at": datetime.now(UTC).isoformat(),
            "method": "configurable weighted score; risk/burden components are opportunity-oriented",
            "domains": results,
        }
        json_text = json.dumps(payload, indent=2, ensure_ascii=False) + "\n"
        (self.report_dir / "domain-research-report.json").write_text(json_text, encoding="utf-8")
        lines = [
            "# Jneopallium domain research report",
            "",
            "This ranking prioritizes repository evidence, runnable demonstrations, integration fit, "
            "and bounded proof-of-concept feasibility. Market size alone is not a ranking factor.",
            "",
            "Risk and regulatory-burden component scores are opportunity-oriented: a higher number means "
            "lower burden or a safer initial pilot.",
            "",
            "| Rank | Domain | Score | Allocation | Repository evidence |",
            "|---:|---|---:|---|---:|",
        ]
        for row in results:
            lines.append(
                f"| {row['rank']} | {row['name']} | {row['score']:.2f} | {row['allocation']} | {len(row['repository_evidence'])} |"
            )
        lines.extend(
            [
                "",
                "## Recommended first pilot",
                "",
                "Run a small industrial automation/digital-twin integration pilot first if it remains in the "
                "top tier: it combines repository demos, FMI/OPC UA/MQTT bridges, deterministic synthetic data, "
                "and a clear safety boundary. Ad-fraud is the parallel research pilot because it supports a "
                "synthetic, non-offensive comparison without accreditation or outcome claims.",
                "",
                "## Limitations",
                "",
                "Scores are prioritization hypotheses, not market forecasts. External evidence is limited to "
                "recorded authoritative sources and must be refreshed before LIVE outreach. Medical, UAV, BCI, "
                "public-safety, and other regulated/high-consequence work remains simulation, read-only, or "
                "human-reviewed unless independently validated.",
                "",
            ]
        )
        markdown = "\n".join(lines)
        (self.report_dir / "domain-research-report.md").write_text(markdown, encoding="utf-8")
        self.repository_docs_dir.mkdir(parents=True, exist_ok=True)
        (self.repository_docs_dir / "domain-research-report.md").write_text(
            markdown, encoding="utf-8"
        )
