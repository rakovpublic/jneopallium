from __future__ import annotations

import json
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.state_machine.service import CampaignState, transition
from jneo_campaign.storage.models import (
    Capability,
    Demo,
    DemoPlan,
    Offer,
    Organization,
    ProspectScore,
)


class DemoMatchingService:
    def __init__(
        self,
        raw_config: dict[str, Any],
        report_dir: Path,
        score_threshold: float,
        engineering_budget_hours: int,
    ) -> None:
        self.config = raw_config
        self.report_dir = report_dir
        self.score_threshold = score_threshold
        self.engineering_budget_hours = engineering_budget_hours

    def match(self, session: Session) -> list[DemoPlan]:
        self._seed_demos(session)
        plans: list[DemoPlan] = []
        for offer in session.scalars(select(Offer).order_by(Offer.organization_id)):
            organization = session.get(Organization, offer.organization_id)
            score = session.scalar(
                select(ProspectScore).where(ProspectScore.organization_id == offer.organization_id)
            )
            if organization is None or score is None:
                continue
            rule = self._rule(organization.target_domain)
            demo = session.scalar(select(Demo).where(Demo.demo_id == rule.get("demo_id")))
            disposition = rule["disposition"]
            engineering_hours = 8 if demo else int(rule.get("estimated_engineering_hours", 32))
            safe = bool(
                demo is None
                and score.total >= self.score_threshold
                and engineering_hours <= self.engineering_budget_hours
            )
            plan_payload = self._plan_payload(
                organization, offer, demo, disposition, engineering_hours, safe
            )
            item = session.scalar(
                select(DemoPlan).where(DemoPlan.organization_id == organization.id)
            )
            if item is None:
                item = DemoPlan(
                    organization_id=organization.id,
                    existing_demo_id=demo.id if demo else None,
                    disposition=disposition,
                    plan=plan_payload,
                    engineering_hours=engineering_hours,
                    safe_to_auto_implement=safe,
                )
                session.add(item)
            else:
                item.existing_demo_id = demo.id if demo else None
                item.disposition = disposition
                item.plan = plan_payload
                item.engineering_hours = engineering_hours
                item.safe_to_auto_implement = safe
            target_state = CampaignState.DEMO_SELECTED if demo else CampaignState.DEMO_SPEC_REQUIRED
            transition(
                session,
                entity_type="prospect",
                entity_id=organization.id,
                to_state=target_state,
                reason=f"{disposition} based on target domain, score, safety, and engineering budget",
                source=f"demo-matching:{rule.get('demo_id', 'fallback')}",
            )
            plans.append(item)
        session.flush()
        self._write_report(plans)
        return plans

    def _seed_demos(self, session: Session) -> None:
        for capability in session.scalars(
            select(Capability).where(Capability.capability_id.like("demo.%"))
        ):
            item = session.scalar(select(Demo).where(Demo.demo_id == capability.capability_id))
            if item is None:
                command_windows = capability.runnable_demo_command or "See capability registry"
                command_posix = command_windows.replace(".ps1", ".sh").replace(
                    "scripts/", "scripts/"
                )
                item = Demo(
                    demo_id=capability.capability_id,
                    name=capability.name,
                    domains=[capability.domain],
                    command_windows=command_windows,
                    command_posix=command_posix,
                    safety_mode=", ".join(capability.safety_constraints)
                    or "SIMULATION_OR_ADVISORY",
                    evidence_refs=capability.documentation + capability.test_evidence,
                )
                session.add(item)
        session.flush()

    def _rule(self, domain: str) -> dict[str, Any]:
        for rule in self.config.get("rules", []):
            if domain in rule["domains"]:
                return rule
        return self.config["fallback"]

    @staticmethod
    def _plan_payload(
        organization: Organization,
        offer: Offer,
        demo: Demo | None,
        disposition: str,
        engineering_hours: int,
        safe: bool,
    ) -> dict[str, Any]:
        regulated = any(
            term in organization.target_domain.lower()
            for term in ("clinical", "medical", "patient", "uav", "bci")
        )
        return {
            "customer_problem": offer.problem,
            "proposed_scenario": (
                f"Replay a deterministic synthetic {organization.target_domain} scenario and compare "
                "multi-timescale evidence handling with transparent baselines."
            ),
            "why_jneopallium_may_be_suitable": (
                "Typed signals, separate fast/slow loops, protocol bridges, and auditable safety gates "
                "can be evaluated without claiming a production outcome."
            ),
            "existing_reusable_modules": offer.capability_ids,
            "existing_demo": demo.demo_id if demo else None,
            "required_new_modules": []
            if demo
            else ["Synthetic scenario adapter", "Domain-specific result schema"],
            "required_bridges": demo.domains
            if demo
            else ["To be confirmed from the prospect's documented interface"],
            "synthetic_data_plan": "Generate deterministic, non-personal, non-operational events with fixed seeds.",
            "architecture": "source --> typed inputs --> fast/slow evidence --> safety gate --> advisory output --> audit log",
            "expected_outputs": [
                "decision JSONL",
                "evidence trail",
                "baseline comparison",
                "limitations report",
            ],
            "comparison_baseline": "Simple threshold rules plus a conventional deterministic statistical score",
            "success_metrics": [
                "deterministic replay",
                "schema validity",
                "evidence completeness",
                "false-positive/negative matrix on labeled synthetic cases",
            ],
            "safety_constraints": [
                "Synthetic data only",
                "No autonomous external actuation",
                "Human review required",
                *(["No diagnosis, treatment, or real patient data"] if regulated else []),
            ],
            "estimated_engineering_hours": engineering_hours,
            "unresolved_assumptions": [
                "Prospect interface and evaluation dataset are unknown",
                "No production performance or regulatory status is assumed",
            ],
            "implementation_milestones": [
                "schema agreement",
                "synthetic fixture",
                "adapter",
                "baseline",
                "deterministic test",
                "review",
            ],
            "auto_implementation_eligible": safe,
            "disposition": disposition,
        }

    def _write_report(self, plans: list[DemoPlan]) -> None:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        payload = {
            "generated_at": datetime.now(UTC).isoformat(),
            "plans": [
                {
                    "organization_id": item.organization_id,
                    "demo_id": item.demo.demo_id if item.demo else None,
                    "disposition": item.disposition,
                    "engineering_hours": item.engineering_hours,
                    "safe_to_auto_implement": item.safe_to_auto_implement,
                    "plan": item.plan,
                }
                for item in plans
            ],
        }
        (self.report_dir / "demo-plans.json").write_text(
            json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
