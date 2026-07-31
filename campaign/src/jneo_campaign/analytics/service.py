from __future__ import annotations

import json
from collections import Counter, defaultdict
from datetime import UTC, datetime
from pathlib import Path
from statistics import mean
from typing import Any

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.storage.models import (
    AuditEvent,
    Campaign,
    ComplianceDecision,
    Contact,
    DemoPlan,
    EmailMessage,
    EmailThread,
    GeneratedAsset,
    Meeting,
    MetricSnapshot,
    Offer,
    Organization,
    ProspectScore,
    ProviderFailure,
    ReplyClassification,
    SuppressionEntry,
)


class AnalyticsService:
    def __init__(self, report_dir: Path) -> None:
        self.report_dir = report_dir.resolve()

    def report(self, session: Session, campaign_key: str) -> dict[str, Any]:
        campaign = session.scalar(select(Campaign).where(Campaign.campaign_key == campaign_key))
        organizations = list(session.scalars(select(Organization)))
        contacts = list(session.scalars(select(Contact)))
        scores = list(session.scalars(select(ProspectScore)))
        threads = list(session.scalars(select(EmailThread)))
        messages = list(session.scalars(select(EmailMessage)))
        classifications = list(session.scalars(select(ReplyClassification)))
        meetings = list(session.scalars(select(Meeting)))
        compliance = list(session.scalars(select(ComplianceDecision)))
        demos = list(session.scalars(select(DemoPlan)))
        outbound = [item for item in messages if item.direction == "OUTBOUND"]
        initial = [item for item in outbound if item.sequence_number == 0]
        delivered = [item for item in initial if item.status in {"DELIVERED", "MOCK_SENT"}]
        bounced = [item for item in messages if item.status == "BOUNCED"]
        inbound = [item for item in messages if item.direction == "INBOUND"]
        positive_labels = {
            "POSITIVE_INTEREST",
            "MEETING_REQUEST",
            "PROPOSAL_REQUEST",
            "POC_REQUEST",
        }
        positive = [item for item in classifications if item.classification in positive_labels]
        classification_counts = Counter(item.classification for item in classifications)
        thread_by_id = {item.id: item for item in threads}
        message_by_id = {item.id: item for item in messages}
        offers = list(session.scalars(select(Offer)))
        offer_by_org = {item.organization_id: item.offer_type for item in offers}
        demo_by_org = {item.organization_id: item.disposition for item in demos}
        persona_by_org: dict[int, str] = {}
        for decision in compliance:
            if decision.decision not in {"APPROVED", "APPROVED_DRY_RUN"}:
                continue
            asset = session.get(GeneratedAsset, decision.asset_id)
            if asset is not None:
                persona_by_org[decision.organization_id] = asset.persona
        positive_thread_ids = {
            message.thread_id
            for item in positive
            if (message := message_by_id.get(item.message_id)) is not None
        }
        conversion = self._conversion_dimensions(
            organizations=organizations,
            initial=initial,
            inbound=inbound,
            thread_by_id=thread_by_id,
            positive_thread_ids=positive_thread_ids,
            offer_by_org=offer_by_org,
            demo_by_org=demo_by_org,
            persona_by_org=persona_by_org,
        )
        disqualification_reasons = Counter(
            item.reason or "unspecified"
            for item in session.scalars(
                select(AuditEvent).where(AuditEvent.to_state == "DISQUALIFIED")
            )
        )
        stage_durations = self._stage_durations(session)
        metrics: dict[str, Any] = {
            "generated_at": datetime.now(UTC).isoformat(),
            "campaign_key": campaign_key,
            "mode": "DRY_RUN"
            if any(item.status == "MOCK_SENT" for item in messages)
            else "LIVE_OR_PREPARED",
            "organizations_discovered": len(organizations),
            "organizations_verified": sum(item.verified for item in organizations),
            "verified_contacts": sum(item.public_evidence_verified for item in contacts),
            "prospects_scored": len(scores),
            "messages_prepared": len(initial),
            "messages_sent_or_simulated": len(initial),
            "real_external_messages_sent": sum(
                item.status in {"SENT", "DELIVERED"} for item in initial
            ),
            "mock_messages_sent": sum(item.status == "MOCK_SENT" for item in initial),
            "delivery_rate": self._rate(len(delivered), len(initial)),
            "bounce_rate": self._rate(len(bounced), len(initial)),
            "reply_rate": self._rate(len({item.thread_id for item in inbound}), len(initial)),
            "positive_reply_rate": self._rate(len(positive_thread_ids), len(initial)),
            "meeting_rate": self._rate(
                sum(item.status == "MEETING_SCHEDULED" for item in meetings), len(initial)
            ),
            "proposal_rate": self._rate(classification_counts["PROPOSAL_REQUEST"], len(initial)),
            "proof_of_concept_requests": classification_counts["POC_REQUEST"],
            "unsubscribe_rate": self._rate(classification_counts["UNSUBSCRIBE"], len(initial)),
            "compliance_blocks": sum(
                item.decision not in {"APPROVED", "APPROVED_DRY_RUN"} for item in compliance
            ),
            "suppression_entries": len(
                list(
                    session.scalars(
                        select(SuppressionEntry).where(SuppressionEntry.active.is_(True))
                    )
                )
            ),
            "common_objections_and_questions": dict(classification_counts.most_common()),
            "conversion_by_domain": conversion["domain"],
            "conversion_by_persona": conversion["persona"],
            "conversion_by_offer": conversion["offer"],
            "conversion_by_demo": conversion["demo"],
            "conversion_by_region": conversion["region"],
            "disqualification_reasons": dict(disqualification_reasons),
            "average_hours_between_stages": stage_durations,
            "research_cost_usd": 0.0,
            "llm_cost_usd": 0.0,
            "estimated_demo_engineering_hours": sum(item.engineering_hours for item in demos),
            "provider_failures": len(
                list(
                    session.scalars(
                        select(ProviderFailure).where(ProviderFailure.resolved.is_(False))
                    )
                )
            ),
            "objective_hierarchy": [
                "legitimate positive conversations",
                "technically relevant meetings",
                "proof-of-concept opportunities",
                "integration opportunities",
                "qualified investor conversations",
                "reply rate",
                "send volume",
            ],
        }
        if campaign:
            session.add(MetricSnapshot(campaign_id=campaign.id, metrics=metrics, dimensions={}))
        self._write(metrics, session)
        return metrics

    @classmethod
    def _conversion_dimensions(
        cls,
        *,
        organizations: list[Organization],
        initial: list[EmailMessage],
        inbound: list[EmailMessage],
        thread_by_id: dict[int, EmailThread],
        positive_thread_ids: set[int],
        offer_by_org: dict[int, str],
        demo_by_org: dict[int, str],
        persona_by_org: dict[int, str],
    ) -> dict[str, dict[str, dict[str, int | float]]]:
        def bucket() -> dict[str, int]:
            return {"prospects": 0, "sent": 0, "replied": 0, "positive_replies": 0}

        result: dict[str, defaultdict[str, dict[str, int]]] = {
            key: defaultdict(bucket) for key in ("domain", "persona", "offer", "demo", "region")
        }
        dimensions_by_org: dict[int, dict[str, str]] = {}
        for organization in organizations:
            dimensions = {
                "domain": organization.target_domain,
                "region": organization.region or "UNKNOWN",
            }
            if organization.id in persona_by_org:
                dimensions["persona"] = persona_by_org[organization.id]
            if organization.id in offer_by_org:
                dimensions["offer"] = offer_by_org[organization.id]
            if organization.id in demo_by_org:
                dimensions["demo"] = demo_by_org[organization.id]
            dimensions_by_org[organization.id] = dimensions
            for dimension, value in dimensions.items():
                result[dimension][value]["prospects"] += 1

        sent_orgs = {
            thread.organization_id
            for message in initial
            if (thread := thread_by_id.get(message.thread_id)) is not None
        }
        replied_orgs = {
            thread.organization_id
            for message in inbound
            if (thread := thread_by_id.get(message.thread_id)) is not None
        }
        positive_orgs = {
            thread.organization_id
            for thread_id in positive_thread_ids
            if (thread := thread_by_id.get(thread_id)) is not None
        }
        for organizations_set, metric in (
            (sent_orgs, "sent"),
            (replied_orgs, "replied"),
            (positive_orgs, "positive_replies"),
        ):
            for organization_id in organizations_set:
                for dimension, value in dimensions_by_org.get(organization_id, {}).items():
                    result[dimension][value][metric] += 1

        finalized: dict[str, dict[str, dict[str, int | float]]] = {}
        for dimension, values in result.items():
            finalized[dimension] = {}
            for value, counts in values.items():
                finalized[dimension][value] = {
                    **counts,
                    "positive_conversion_rate": cls._rate(
                        counts["positive_replies"], counts["sent"]
                    ),
                }
        return finalized

    @staticmethod
    def _rate(numerator: int, denominator: int) -> float:
        return round(numerator / denominator, 4) if denominator else 0.0

    @staticmethod
    def _stage_durations(session: Session) -> dict[str, float]:
        grouped: dict[tuple[str, str], list[AuditEvent]] = defaultdict(list)
        for event in session.scalars(
            select(AuditEvent)
            .where(AuditEvent.action == "STATE_TRANSITION")
            .order_by(AuditEvent.occurred_at)
        ):
            grouped[(event.entity_type, event.entity_id)].append(event)
        durations: dict[str, list[float]] = defaultdict(list)
        for events in grouped.values():
            for prior, current in zip(events, events[1:], strict=False):
                key = f"{prior.to_state}->{current.to_state}"
                delta = current.occurred_at - prior.occurred_at
                durations[key].append(delta.total_seconds() / 3600)
        return {key: round(mean(values), 3) for key, values in durations.items()}

    def _write(self, metrics: dict[str, Any], session: Session) -> None:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        (self.report_dir / "campaign-analytics.json").write_text(
            json.dumps(metrics, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
        lines = [
            "# Campaign analytics",
            "",
            f"Generated: {metrics['generated_at']}",
            "",
            "| Metric | Value |",
            "|---|---:|",
        ]
        for key in (
            "organizations_discovered",
            "organizations_verified",
            "verified_contacts",
            "prospects_scored",
            "messages_prepared",
            "real_external_messages_sent",
            "mock_messages_sent",
            "delivery_rate",
            "bounce_rate",
            "reply_rate",
            "positive_reply_rate",
            "meeting_rate",
            "unsubscribe_rate",
            "compliance_blocks",
        ):
            lines.append(f"| {key.replace('_', ' ').title()} | {metrics[key]} |")
        lines.extend(
            [
                "",
                "The objective hierarchy prioritizes legitimate technical conversations and meetings "
                "over reply rate or send volume. No experiment winner is selected without its configured "
                "minimum sample size.",
                "",
            ]
        )
        (self.report_dir / "campaign-analytics.md").write_text("\n".join(lines), encoding="utf-8")
        audit = [
            {
                "event_id": item.event_id,
                "occurred_at": item.occurred_at.isoformat(),
                "actor": item.actor,
                "action": item.action,
                "entity_type": item.entity_type,
                "entity_id": item.entity_id,
                "from_state": item.from_state,
                "to_state": item.to_state,
                "reason": item.reason,
                "source": item.source,
            }
            for item in session.scalars(select(AuditEvent).order_by(AuditEvent.occurred_at))
        ]
        (self.report_dir / "audit-report.json").write_text(
            json.dumps({"events": audit}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
