from __future__ import annotations

import hashlib
import re
from pathlib import Path
from typing import Any

from jinja2 import Environment, FileSystemLoader, StrictUndefined, select_autoescape
from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.asset_generation.schemas import PropositionOutput
from jneo_campaign.providers.interfaces import StructuredLLMProvider
from jneo_campaign.security import text_to_safe_html
from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Capability,
    Contact,
    DemoPlan,
    GeneratedAsset,
    Offer,
    Organization,
    OrganizationSource,
    ProspectScore,
)

PERSONAS: dict[str, str] = {
    "cto_vp_engineering": "Focus on architecture boundaries, integration risk, and repeatable verification.",
    "product_innovation_lead": "Focus on a bounded pilot, user value, and a reversible next step.",
    "system_architect": "Focus on schemas, bridges, deployment boundaries, and auditability.",
    "research_director": "Focus on hypotheses, baselines, reproducibility, and publishable negative results.",
    "clinical_informatics_lead": "Focus on FHIR/DICOM context, advisory use, evidence trails, and clinician review.",
    "cybersecurity_lead": "Focus on analyst triage, telemetry integration, false positives, and no automatic response.",
    "adtech_fraud_lead": "Focus on IVT evidence, fast/slow signals, supply path context, and measured synthetic comparison.",
    "operations_lead": "Focus on observable operational pain, bounded advisory output, and existing control ownership.",
    "investor": "Focus on reusable architecture, evidence-backed readiness, experiments, risks, and milestones.",
    "accelerator_grant_evaluator": "Focus on technical novelty, credible milestones, public benefit, and explicit uncertainty.",
}

SUPPORTING_ASSETS = [
    "follow_up_sequence",
    "integration_proposition",
    "proof_of_concept_specification",
    "research_collaboration_proposition",
    "investor_summary",
    "meeting_agenda",
    "meeting_preparation_brief",
    "objection_handling_notes",
    "technical_faq",
    "demo_instructions",
    "repository_links",
    "architecture_diagram",
]

ENGLISH_OPT_OUT = (
    'If you would prefer not to receive further messages, reply "unsubscribe" and we will '
    "not contact you again."
)
UKRAINIAN_OPT_OUT = (
    'Якщо ви не бажаєте отримувати подальші повідомлення, дайте відповідь "unsubscribe" '
    'або "відписатися", і ми більше не будемо з вами зв’язуватися.'
)


class AssetGenerationService:
    def __init__(
        self,
        template_dir: Path,
        llm_provider: StructuredLLMProvider,
        repository_url: str = "https://github.com/rakovpublic/jneopallium",
    ) -> None:
        self.environment = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=select_autoescape(("html", "xml")),
            undefined=StrictUndefined,
            trim_blocks=True,
            lstrip_blocks=True,
        )
        self.llm_provider = llm_provider
        self.repository_url = repository_url

    def generate(self, session: Session, limit: int = 5) -> list[GeneratedAsset]:
        candidates = self._email_candidates(session)[:limit]
        generated: list[GeneratedAsset] = []
        for _score, organization, contact, offer in candidates:
            plan = session.scalar(
                select(DemoPlan).where(DemoPlan.organization_id == organization.id)
            )
            sources = list(
                session.scalars(
                    select(OrganizationSource).where(
                        OrganizationSource.organization_id == organization.id
                    )
                )
            )
            capabilities = (
                list(
                    session.scalars(
                        select(Capability).where(Capability.capability_id.in_(offer.capability_ids))
                    )
                )
                if offer.capability_ids
                else []
            )
            primary_persona = self._primary_persona(organization.target_domain)
            for persona in PERSONAS:
                proposition = self._proposition(
                    organization, contact, offer, plan, sources, capabilities, persona
                )
                intro = self.environment.get_template("intro_email.txt.j2").render(
                    proposition=proposition,
                    persona_guidance=PERSONAS[persona],
                    demo=plan.demo if plan else None,
                    repository_url=self.repository_url,
                )
                intro = self._localize_opt_out(intro, contact.locale)
                generated.extend(
                    self._store_formats(
                        session,
                        organization.id,
                        "introductory_email",
                        persona,
                        intro,
                        proposition.evidence_refs,
                    )
                )
                technical = self.environment.get_template("technical_proposition.md.j2").render(
                    proposition=proposition,
                    persona_guidance=PERSONAS[persona],
                    demo_plan=plan.plan if plan else {},
                    repository_url=self.repository_url,
                )
                generated.extend(
                    self._store_formats(
                        session,
                        organization.id,
                        "one_page_technical_proposition",
                        persona,
                        technical,
                        proposition.evidence_refs,
                    )
                )
                if persona == primary_persona:
                    for asset_type in SUPPORTING_ASSETS:
                        content = self._supporting_content(asset_type, proposition, plan)
                        generated.extend(
                            self._store_formats(
                                session,
                                organization.id,
                                asset_type,
                                persona,
                                content,
                                proposition.evidence_refs,
                            )
                        )
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=CampaignState.MATERIALS_GENERATED,
                reason="Structured, evidence-cited assets generated and schema-validated",
                source=f"asset-generator:{self.llm_provider.name}",
            )
        session.flush()
        return generated

    def _email_candidates(
        self, session: Session
    ) -> list[tuple[ProspectScore, Organization, Contact, Offer]]:
        result: list[tuple[ProspectScore, Organization, Contact, Offer]] = []
        for score in session.scalars(select(ProspectScore).order_by(ProspectScore.total.desc())):
            organization = session.get(Organization, score.organization_id)
            if organization is None or organization.paused:
                continue
            contact = session.scalar(
                select(Contact).where(
                    Contact.organization_id == organization.id,
                    Contact.channel_type == "email",
                    Contact.public_evidence_verified.is_(True),
                )
            )
            offer = session.scalar(select(Offer).where(Offer.organization_id == organization.id))
            if contact and offer:
                result.append((score, organization, contact, offer))
        return result

    def _proposition(
        self,
        organization: Organization,
        contact: Contact,
        offer: Offer,
        plan: DemoPlan | None,
        sources: list[OrganizationSource],
        capabilities: list[Capability],
        persona: str,
    ) -> PropositionOutput:
        source = sources[0]
        capability = capabilities[0] if capabilities else None
        implemented = (
            capability.allowed_claims[0]
            if capability and capability.allowed_claims
            else "A proposed proof of concept would test the fit; no current implementation claim is made."
        )
        refs = list(
            dict.fromkeys(
                [
                    source.source_url,
                    *offer.evidence_refs,
                    *[item.capability_id for item in capabilities],
                ]
            )
        )
        draft: dict[str, Any] = {
            "organization": organization.name,
            "persona": persona,
            "subject_line": self._subject_line(organization.name, organization.target_domain),
            "specific_activity": organization.summary,
            "activity_evidence": self._relevance_clause(
                source.supporting_excerpt, organization.summary
            ),
            "relevant_capability": offer.proposition,
            "implemented_now": self._implemented_claim(implemented),
            "proposed_work": (
                self._plain_scenario(plan.plan["proposed_scenario"])
                if plan
                else "design a small evaluation using synthetic data"
            ),
            "next_step": "Send a two-page test plan before requesting a meeting.",
            "poc_inputs": (plan.plan.get("synthetic_data_plan", "Synthetic events"),),
            "poc_outputs": self._plain_outputs(
                plan.plan.get("expected_outputs", ["evidence trail"])
            ),
            "limitations": list(
                dict.fromkeys(
                    offer.limitations + (plan.plan.get("safety_constraints", []) if plan else [])
                )
            ),
            "scope_boundary": self._scope_boundary(organization.target_domain),
            "call_to_action": (
                "Would it be useful if I sent that plan first? If another team handles this work, "
                "a redirect would be appreciated."
            ),
            "evidence_refs": refs,
        }
        return self.llm_provider.generate(
            schema=PropositionOutput,
            task=(
                f"Produce an evidence-grounded proposition for persona {persona}. Do not add facts, "
                "claims, customers, outcomes, certifications, or readiness assertions."
            ),
            evidence=[{"draft": draft}],
        )

    @staticmethod
    def _subject_line(organization: str, domain: str) -> str:
        value = domain.lower()
        if "digital twin" in value:
            topic = "FMI digital-twin test plan"
        elif any(term in value for term in ("clinical", "medical", "health", "patient")):
            topic = "Synthetic health-workflow test plan"
        elif "cyber" in value or "security" in value:
            topic = "Synthetic cyber-triage test plan"
        elif "ad fraud" in value or "fraud" in value or re.search(r"\bbot\b", value):
            topic = "Synthetic anomaly-detection test plan"
        elif any(term in value for term in ("robot", "uav", "autonomy")):
            topic = "Reproducible autonomy test plan"
        elif any(term in value for term in ("industrial", "process supervision")):
            topic = "Synthetic industrial-control test plan"
        elif "education" in value or "tutoring" in value:
            topic = "Adaptive-learning integration test plan"
        else:
            topic = f"Reproducible {domain.lower()} test plan"
        candidate = f"{topic} for {organization}"
        return candidate if len(candidate) <= 100 else topic

    @staticmethod
    def _relevance_clause(excerpt: str, summary: str) -> str:
        value = re.sub(r"\s+", " ", excerpt).strip().rstrip(".")
        match = re.match(
            r"^The official .+? (describes|publishes|presents|identifies|states|invites) (.+)$",
            value,
            flags=re.IGNORECASE,
        )
        if match:
            verb, detail = match.groups()
            normalized_verbs = {
                "describes": "describe",
                "publishes": "publish",
                "presents": "present",
                "identifies": "identify",
                "states": "state that",
                "invites": "invite",
            }
            return f"your public materials {normalized_verbs[verb.lower()]} {detail}"

        fallback = re.sub(r"\s+", " ", summary).strip().rstrip(".")
        if fallback.lower().startswith(("a ", "an ", "the ")):
            fallback = fallback[0].lower() + fallback[1:]
            return f"your public materials describe {fallback}"
        return f"your public materials explain that {fallback}"

    @staticmethod
    def _plain_scenario(scenario: str) -> str:
        value = re.sub(r"\s+", " ", scenario).strip().rstrip(".")
        value = re.sub(
            r"^Replay a deterministic synthetic ",
            "run a repeatable synthetic ",
            value,
            flags=re.IGNORECASE,
        )
        value = value.replace("digital twins scenario", "digital-twin scenario")
        value = value.replace(
            " and compare multi-timescale evidence handling with transparent baselines",
            " and compare its decision trail with a simple rules-based baseline",
        )
        return value[0].lower() + value[1:] if value else "run a synthetic evaluation"

    @staticmethod
    def _plain_outputs(outputs: list[str]) -> list[str]:
        replacements = {
            "decision JSONL": "decision log",
            "limitations report": "clear limitations",
        }
        return [replacements.get(item, item) for item in outputs]

    @staticmethod
    def _implemented_claim(claim: str) -> str:
        match = re.fullmatch(
            r"The repository contains an implementation of (.+); customer-specific validation is required\.",
            claim,
        )
        if not match:
            return claim
        capability = match.group(1)
        article = "an" if capability[0].upper() in set("AEFHILMNORSX") else "a"
        return (
            f"The repository includes {article} {capability}; adapting it to your environment "
            "would require validation."
        )

    @staticmethod
    def _scope_boundary(domain: str) -> str:
        value = domain.lower()
        if any(term in value for term in ("clinical", "medical", "health", "patient")):
            return (
                "The evaluation would use synthetic data, remain advisory, and would not diagnose, "
                "prescribe, or claim clinical effectiveness."
            )
        if "cyber" in value or "security" in value or "fraud" in value:
            return (
                "The evaluation would use synthetic data and produce advisory evidence only; it "
                "would not take automatic action or claim production performance."
            )
        if any(term in value for term in ("robot", "uav", "autonomy")):
            return "The evaluation would remain simulation-only and would not control real equipment."
        return (
            "The evaluation would use deterministic synthetic data and would not be evidence of "
            "production deployment."
        )

    @staticmethod
    def _localize_opt_out(intro: str, locale: str) -> str:
        if locale == "uk":
            return intro.replace(ENGLISH_OPT_OUT, UKRAINIAN_OPT_OUT)
        return intro

    def _store_formats(
        self,
        session: Session,
        organization_id: int,
        asset_type: str,
        persona: str,
        markdown: str,
        evidence_refs: list[str],
    ) -> list[GeneratedAsset]:
        result = []
        for format_name, content in (
            ("markdown", markdown.strip() + "\n"),
            ("html", text_to_safe_html(markdown.strip())),
        ):
            digest = hashlib.sha256(content.encode()).hexdigest()
            item = session.scalar(
                select(GeneratedAsset).where(
                    GeneratedAsset.organization_id == organization_id,
                    GeneratedAsset.asset_type == asset_type,
                    GeneratedAsset.persona == persona,
                    GeneratedAsset.format == format_name,
                )
            )
            if item is None:
                item = GeneratedAsset(
                    organization_id=organization_id,
                    asset_type=asset_type,
                    persona=persona,
                    format=format_name,
                    content=content,
                    evidence_refs=evidence_refs,
                    content_hash=digest,
                    validated=True,
                )
                session.add(item)
            else:
                item.content = content
                item.evidence_refs = evidence_refs
                item.content_hash = digest
                item.validated = True
            result.append(item)
        return result

    def _supporting_content(
        self, asset_type: str, proposition: PropositionOutput, plan: DemoPlan | None
    ) -> str:
        if asset_type == "architecture_diagram":
            return self.environment.get_template("architecture.md.j2").render(
                proposition=proposition
            )
        if asset_type == "follow_up_sequence":
            return self.environment.get_template("followups.md.j2").render(
                proposition=proposition, demo=plan.demo if plan else None
            )
        title = asset_type.replace("_", " ").title()
        sections = {
            "integration_proposition": [
                "Interface assumptions",
                "Schema mapping",
                "Safety boundary",
                "Acceptance test",
            ],
            "proof_of_concept_specification": [
                "Scenario",
                "Inputs",
                "Outputs",
                "Baseline",
                "Metrics",
                "Milestones",
            ],
            "research_collaboration_proposition": [
                "Hypothesis",
                "Reproducibility",
                "Negative results",
                "Publication and IP discussion",
            ],
            "investor_summary": [
                "Reusable architecture",
                "Evidence-backed readiness",
                "Go-to-market experiment",
                "Risks",
                "Funding milestones",
            ],
            "meeting_agenda": [
                "Problem evidence",
                "Current capability",
                "Integration boundary",
                "Synthetic pilot",
                "Decision and owners",
            ],
            "meeting_preparation_brief": [
                "Verified activity",
                "Questions to validate",
                "Claims to avoid",
                "Proposed decision",
            ],
            "objection_handling_notes": [
                "Why another framework?",
                "Production readiness",
                "Performance evidence",
                "Security and compliance",
            ],
            "technical_faq": [
                "What exists?",
                "What is proposed?",
                "What data is needed?",
                "How is safety handled?",
            ],
            "demo_instructions": [
                "Prerequisites",
                "Run command",
                "Expected artifacts",
                "Limitations",
            ],
            "repository_links": [
                "Repository",
                "Capability evidence",
                "Demo evidence",
                "Documentation",
            ],
        }
        lines = [f"# {title}: {proposition.organization}", ""]
        for heading in sections.get(asset_type, ["Scope"]):
            lines.extend([f"## {heading}", ""])
            if heading == "Inputs":
                lines.extend([f"- {item}" for item in proposition.poc_inputs] + [""])
            elif heading == "Outputs":
                lines.extend([f"- {item}" for item in proposition.poc_outputs] + [""])
            elif heading == "Limitations" or "avoid" in heading.lower():
                lines.extend([f"- {item}" for item in proposition.limitations] + [""])
            elif heading == "Run command":
                command = (
                    plan.demo.command_windows
                    if plan and plan.demo
                    else "A new demo command is not implemented."
                )
                lines.extend([f"`{command}`", ""])
            elif heading == "Repository":
                lines.extend([self.repository_url, ""])
            else:
                lines.extend(
                    [
                        proposition.proposed_work,
                        "",
                        f"Evidence: {', '.join(proposition.evidence_refs)}",
                        "",
                    ]
                )
        return "\n".join(lines)

    @staticmethod
    def _primary_persona(domain: str) -> str:
        value = domain.lower()
        if "ad fraud" in value:
            return "adtech_fraud_lead"
        if any(term in value for term in ("clinical", "medical", "health")):
            return "clinical_informatics_lead"
        if "cyber" in value or "security" in value:
            return "cybersecurity_lead"
        if "industrial" in value:
            return "system_architect"
        if any(term in value for term in ("robot", "uav")):
            return "system_architect"
        return "research_director"
