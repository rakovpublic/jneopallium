from __future__ import annotations

import hashlib
import logging
from collections.abc import Callable
from datetime import UTC, datetime
from typing import Any

from apscheduler.schedulers.blocking import BlockingScheduler
from sqlalchemy import select
from sqlalchemy.orm import Session
from tenacity import Retrying, stop_after_attempt, wait_exponential

from jneo_campaign.analytics.service import AnalyticsService
from jneo_campaign.asset_generation.service import AssetGenerationService
from jneo_campaign.calendar.service import MeetingCoordinator
from jneo_campaign.capability_registry.service import CapabilityRegistryBuilder
from jneo_campaign.compliance.service import ComplianceService
from jneo_campaign.config import CAMPAIGN_ROOT, AppConfig, load_config
from jneo_campaign.demo_matching.service import DemoMatchingService
from jneo_campaign.domain_research.service import DomainResearchService
from jneo_campaign.experiments.service import ExperimentService
from jneo_campaign.followups.service import FollowUpService
from jneo_campaign.gmail.service import GmailOutreachService
from jneo_campaign.offer_generation.service import OfferGenerationService
from jneo_campaign.prospect_discovery.service import ProspectDiscoveryService
from jneo_campaign.providers.factory import Providers, build_providers
from jneo_campaign.providers.mock import MockGmailProvider
from jneo_campaign.reply_processing.service import ReplyProcessingService
from jneo_campaign.scoring.service import ProspectScoringService
from jneo_campaign.security import redact_secrets
from jneo_campaign.storage.database import Database
from jneo_campaign.storage.models import (
    Contact,
    EmailMessage,
    EmailThread,
    JobRun,
    Meeting,
    ProviderFailure,
)
from jneo_campaign.verification.service import VerificationService

LOGGER = logging.getLogger("jneo_campaign")


class CampaignOrchestrator:
    def __init__(
        self,
        config: AppConfig | None = None,
        providers: Providers | None = None,
    ) -> None:
        self.config = config or load_config()
        self.database = Database(self.config.settings.campaign_database_url)
        self.database.create_all()
        self.providers = providers or build_providers(self.config)
        self.inventory = CapabilityRegistryBuilder(
            self.config.settings.campaign_repository_root,
            self.config.settings.campaign_report_dir,
        )
        self.domains = DomainResearchService(
            self.config.raw_domains,
            self.config.settings.campaign_report_dir,
            self.config.settings.campaign_repository_root / "docs",
        )
        self.discovery = ProspectDiscoveryService(self.providers.search)
        self.verification = VerificationService()
        self.scoring = ProspectScoringService(
            self.config.campaign.target_regions,
            self.config.campaign.excluded_regions,
        )
        self.offers = OfferGenerationService(
            self.config.raw_offers, self.config.campaign.minimum_prospect_score
        )
        self.demos = DemoMatchingService(
            self.config.raw_demo_matching,
            self.config.settings.campaign_report_dir,
            self.config.campaign.demo_auto_plan_threshold,
            self.config.campaign.demo_engineering_budget_hours,
        )
        self.assets = AssetGenerationService(CAMPAIGN_ROOT / "templates", self.providers.llm)
        self.compliance = ComplianceService(self.config.settings, self.config.compliance)
        self.outreach = GmailOutreachService(
            self.config.settings, self.config.campaign, self.providers.gmail
        )
        self.replies = ReplyProcessingService(self.providers.gmail)
        self.followups = FollowUpService(
            self.config.settings, self.config.campaign, self.providers.gmail
        )
        self.meetings = MeetingCoordinator(
            self.config.settings, self.providers.calendar, self.providers.gmail
        )
        self.analytics = AnalyticsService(self.config.settings.campaign_report_dir)
        self.experiments = ExperimentService()

    def run_once(self, simulate_replies: bool | None = None) -> dict[str, Any]:
        if simulate_replies is None:
            simulate_replies = not self.config.settings.live_writes_enabled
        cycle = datetime.now(UTC).isoformat(timespec="seconds")
        results: dict[str, Any] = {
            "cycle": cycle,
            "mode": self.config.settings.campaign_mode,
            "live_writes_enabled": self.config.settings.live_writes_enabled,
            "stages": {},
        }

        stages: list[tuple[str, Callable[[Session], Any]]] = [
            ("inventory", self._inventory),
            ("domain_research", self._domain_research),
            ("prospect_discovery", self._prospect_discovery),
            ("prospect_verification", self.verification.verify),
            ("prospect_scoring", lambda session: {"scored": len(self.scoring.score(session))}),
            ("offer_generation", lambda session: {"offers": len(self.offers.generate(session))}),
            ("demo_matching", lambda session: {"plans": len(self.demos.match(session))}),
            (
                "asset_generation",
                lambda session: {"assets": len(self.assets.generate(session, limit=5))},
            ),
            ("compliance", self._compliance),
            ("outreach_prepare", lambda session: {"prepared": len(self.outreach.prepare(session))}),
            (
                "outreach_send",
                lambda session: {"sent_or_simulated": len(self.outreach.send(session))},
            ),
            (
                "followup_schedule",
                lambda session: {"scheduled": len(self.followups.schedule(session))},
            ),
        ]
        if simulate_replies:
            stages.append(("simulate_replies", self._simulate_initial_replies))
        stages.extend(
            [
                ("inbox_sync", lambda session: {"classified": len(self.replies.sync(session))}),
                (
                    "meeting_options",
                    lambda session: {"proposed": len(self.meetings.propose(session))},
                ),
            ]
        )
        if simulate_replies:
            stages.extend(
                [
                    ("simulate_meeting_agreement", self._simulate_meeting_agreement),
                    (
                        "inbox_resync",
                        lambda session: {"classified": len(self.replies.sync(session))},
                    ),
                ]
            )
        stages.extend(
            [
                (
                    "meeting_confirmation",
                    lambda session: {"scheduled": len(self.meetings.confirm_agreements(session))},
                ),
                ("followup_run", lambda session: {"sent": len(self.followups.run(session))}),
                ("experiments", self._experiments),
                ("analytics", self._analytics),
            ]
        )
        for name, callback in stages:
            results["stages"][name] = self._execute_stage(name, cycle, callback)
        results["acceptance"] = results["stages"].get("analytics", {}).get("result", {})
        return results

    def run_continuous(self, interval_minutes: int = 60) -> None:
        scheduler = BlockingScheduler(timezone="UTC")
        scheduler.add_job(
            self.run_once,
            "interval",
            minutes=interval_minutes,
            id="jneo-campaign-continuous",
            max_instances=1,
            coalesce=True,
            misfire_grace_time=interval_minutes * 60,
            next_run_time=datetime.now(UTC),
        )
        scheduler.start()

    def _execute_stage(
        self, name: str, cycle: str, callback: Callable[[Session], Any]
    ) -> dict[str, Any]:
        key = hashlib.sha256(f"{name}|{cycle}".encode()).hexdigest()
        with self.database.session() as session:
            existing = session.scalar(select(JobRun).where(JobRun.idempotency_key == key))
            if existing and existing.status == "COMPLETED":
                return {"status": "COMPLETED", "result": existing.details, "repeat_safe": True}
            if existing is None:
                session.add(JobRun(job_key=name, idempotency_key=key, status="RUNNING"))
        try:
            value: Any = None
            for attempt in Retrying(
                stop=stop_after_attempt(3),
                wait=wait_exponential(multiplier=0.1, min=0.1, max=2),
                reraise=True,
            ):
                with attempt:
                    with self.database.session() as session:
                        value = callback(session)
            safe_value = redact_secrets(value)
            with self.database.session() as session:
                job = session.scalar(select(JobRun).where(JobRun.idempotency_key == key))
                if job:
                    job.status = "COMPLETED"
                    job.completed_at = datetime.now(UTC)
                    job.attempts = attempt.retry_state.attempt_number
                    job.details = (
                        safe_value if isinstance(safe_value, dict) else {"value": safe_value}
                    )
            return {"status": "COMPLETED", "result": safe_value}
        except Exception as exc:
            redacted = str(redact_secrets(str(exc)))[:2000]
            LOGGER.exception("Campaign stage failed but the operating loop will continue: %s", name)
            with self.database.session() as session:
                job = session.scalar(select(JobRun).where(JobRun.idempotency_key == key))
                if job:
                    job.status = "FAILED"
                    job.completed_at = datetime.now(UTC)
                    job.details = {"error": redacted}
                session.add(
                    ProviderFailure(
                        provider="internal-or-configured-provider",
                        operation=name,
                        error_type=type(exc).__name__,
                        redacted_message=redacted,
                        retryable=True,
                    )
                )
            return {"status": "FAILED", "error": redacted, "continued": True}

    def _inventory(self, session: Session) -> dict[str, Any]:
        registry = self.inventory.build(session)
        return {
            "capabilities": len(registry["capabilities"]),
            "tracked_files_audited": registry["tracked_file_count"],
            "repository_commit": registry["repository_commit"],
        }

    def _domain_research(self, session: Session) -> dict[str, Any]:
        results = self.domains.research(session)
        return {
            "domains_scored": len(results),
            "pursue_now": sum(item["allocation"] == "PURSUE_NOW" for item in results),
            "pilot": sum(item["allocation"] == "PILOT" for item in results),
            "top_domain": results[0]["name"] if results else None,
        }

    def _prospect_discovery(self, session: Session) -> dict[str, int]:
        return self.discovery.discover(session, self.config.campaign.active_domains, limit=50)

    def _compliance(self, session: Session) -> dict[str, Any]:
        decisions = self.compliance.review(session)
        counts: dict[str, int] = {}
        for item in decisions:
            counts[item.decision] = counts.get(item.decision, 0) + 1
        return {"decisions": len(decisions), "by_decision": counts}

    def _simulate_initial_replies(self, session: Session) -> dict[str, int]:
        if not isinstance(self.providers.gmail, MockGmailProvider):
            return {"simulated": 0}
        messages = list(
            session.scalars(
                select(EmailMessage)
                .where(
                    EmailMessage.direction == "OUTBOUND",
                    EmailMessage.sequence_number == 0,
                    EmailMessage.status == "MOCK_SENT",
                )
                .order_by(EmailMessage.id)
            )
        )
        # Use an explicitly recorded timezone with a real sender/recipient business-hour overlap
        # for the meeting simulation; other reply classes remain attached deterministically.
        messages.sort(
            key=lambda item: (
                0
                if (
                    (thread := session.get(EmailThread, item.thread_id))
                    and (contact := session.get(Contact, thread.contact_id))
                    and contact.timezone == "America/New_York"
                )
                else 1
            )
        )
        bodies = [
            "This looks relevant. Could we meet next week to discuss the synthetic evaluation?",
            "Can you share the exact input schema and baseline details?",
            "Please unsubscribe this address.",
            "The bounded research approach sounds promising; please send more information.",
            "Not now; please revisit later this year.",
        ]
        simulated = 0
        existing_ids = {item.message_id for item in self.providers.gmail.inbox}
        for message, body in zip(messages, bodies, strict=False):
            thread = session.get(EmailThread, message.thread_id)
            contact = session.get(Contact, thread.contact_id) if thread else None
            if not thread or not thread.provider_thread_id or not contact:
                continue
            digest = hashlib.sha256(f"{thread.provider_thread_id}|{body}".encode()).hexdigest()[:20]
            if f"mock-reply-{digest}" in existing_ids:
                continue
            self.providers.gmail.add_reply(
                thread_id=thread.provider_thread_id,
                body=body,
                from_address=contact.channel_value,
            )
            simulated += 1
        return {"simulated": simulated}

    def _simulate_meeting_agreement(self, session: Session) -> dict[str, int]:
        if not isinstance(self.providers.gmail, MockGmailProvider):
            return {"simulated": 0}
        meeting = session.scalar(
            select(Meeting).where(Meeting.status == "OPTIONS_PROPOSED").order_by(Meeting.id)
        )
        if meeting is None:
            return {"simulated": 0}
        thread = session.get(EmailThread, meeting.thread_id)
        contact = session.get(Contact, thread.contact_id) if thread else None
        if not thread or not thread.provider_thread_id or not contact:
            return {"simulated": 0}
        body = "I agree to option 1. Please schedule it."
        digest = hashlib.sha256(f"{thread.provider_thread_id}|{body}".encode()).hexdigest()[:20]
        if any(item.message_id == f"mock-reply-{digest}" for item in self.providers.gmail.inbox):
            return {"simulated": 0}
        self.providers.gmail.add_reply(
            thread_id=thread.provider_thread_id,
            body=body,
            from_address=contact.channel_value,
        )
        return {"simulated": 1}

    def _experiments(self, session: Session) -> dict[str, Any]:
        item = self.experiments.ensure_default(session, self.config.campaign.campaign_id)
        return {
            "active": bool(item),
            "minimum_sample_size": item.minimum_sample_size if item else None,
            "winner_selected": bool(item and item.selected_result),
        }

    def _analytics(self, session: Session) -> dict[str, Any]:
        return self.analytics.report(session, self.config.campaign.campaign_id)
