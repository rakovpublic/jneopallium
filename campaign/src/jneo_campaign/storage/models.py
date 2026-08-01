from __future__ import annotations

from datetime import UTC, datetime
from typing import Any

from sqlalchemy import (
    JSON,
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship


def utcnow() -> datetime:
    return datetime.now(UTC)


class Base(DeclarativeBase):
    pass


class TimestampMixin:
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=utcnow, onupdate=utcnow
    )


class Domain(Base, TimestampMixin):
    __tablename__ = "domains"
    id: Mapped[int] = mapped_column(primary_key=True)
    slug: Mapped[str] = mapped_column(String(120), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(240))
    category: Mapped[str] = mapped_column(String(80))
    allocation: Mapped[str] = mapped_column(String(40), default="RESEARCH_MORE")
    score: Mapped[float] = mapped_column(Float, default=0)
    score_components: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)
    score_explanation: Mapped[str] = mapped_column(Text, default="")
    paused: Mapped[bool] = mapped_column(Boolean, default=False)


class DomainResearchFinding(Base, TimestampMixin):
    __tablename__ = "domain_research_findings"
    id: Mapped[int] = mapped_column(primary_key=True)
    domain_id: Mapped[int] = mapped_column(ForeignKey("domains.id", ondelete="CASCADE"))
    fact: Mapped[str] = mapped_column(Text)
    source_url: Mapped[str] = mapped_column(Text)
    source_type: Mapped[str] = mapped_column(String(80))
    supporting_excerpt: Mapped[str] = mapped_column(Text)
    retrieved_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    domain: Mapped[Domain] = relationship()
    __table_args__ = (UniqueConstraint("domain_id", "fact", "source_url"),)


class Capability(Base, TimestampMixin):
    __tablename__ = "capabilities"
    id: Mapped[int] = mapped_column(primary_key=True)
    capability_id: Mapped[str] = mapped_column(String(160), unique=True, index=True)
    domain: Mapped[str] = mapped_column(String(120), index=True)
    name: Mapped[str] = mapped_column(String(240))
    implementation_status: Mapped[str] = mapped_column(String(60))
    source_files: Mapped[list[str]] = mapped_column(JSON, default=list)
    documentation: Mapped[list[str]] = mapped_column(JSON, default=list)
    runnable_demo_command: Mapped[str | None] = mapped_column(Text, nullable=True)
    test_evidence: Mapped[list[str]] = mapped_column(JSON, default=list)
    generated_artifacts: Mapped[list[str]] = mapped_column(JSON, default=list)
    protocols: Mapped[list[str]] = mapped_column(JSON, default=list)
    limitations: Mapped[list[str]] = mapped_column(JSON, default=list)
    safety_constraints: Mapped[list[str]] = mapped_column(JSON, default=list)
    readiness: Mapped[str] = mapped_column(String(60))
    allowed_claims: Mapped[list[str]] = mapped_column(JSON, default=list)
    prohibited_claims: Mapped[list[str]] = mapped_column(JSON, default=list)
    evidence_digest: Mapped[str] = mapped_column(String(64))


class CapabilityEvidence(Base, TimestampMixin):
    __tablename__ = "capability_evidence"
    id: Mapped[int] = mapped_column(primary_key=True)
    capability_id: Mapped[int] = mapped_column(ForeignKey("capabilities.id", ondelete="CASCADE"))
    evidence_type: Mapped[str] = mapped_column(String(40))
    path: Mapped[str] = mapped_column(Text)
    excerpt: Mapped[str] = mapped_column(Text)
    sha256: Mapped[str] = mapped_column(String(64))
    capability: Mapped[Capability] = relationship()
    __table_args__ = (UniqueConstraint("capability_id", "path", "evidence_type"),)


class Organization(Base, TimestampMixin):
    __tablename__ = "organizations"
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(240), index=True)
    canonical_domain: Mapped[str] = mapped_column(String(255), unique=True)
    organization_type: Mapped[str] = mapped_column(String(80))
    prospect_category: Mapped[str] = mapped_column(String(80))
    target_domain: Mapped[str] = mapped_column(String(120), index=True)
    region: Mapped[str] = mapped_column(String(40))
    country: Mapped[str | None] = mapped_column(String(80), nullable=True)
    summary: Mapped[str] = mapped_column(Text)
    verified: Mapped[bool] = mapped_column(Boolean, default=False)
    paused: Mapped[bool] = mapped_column(Boolean, default=False)


class OrganizationSource(Base, TimestampMixin):
    __tablename__ = "organization_sources"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    source_url: Mapped[str] = mapped_column(Text)
    source_type: Mapped[str] = mapped_column(String(80))
    supporting_excerpt: Mapped[str] = mapped_column(Text)
    retrieved_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    source_hash: Mapped[str] = mapped_column(String(64))
    organization: Mapped[Organization] = relationship()
    __table_args__ = (UniqueConstraint("organization_id", "source_url"),)


class Contact(Base, TimestampMixin):
    __tablename__ = "contacts"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    name: Mapped[str | None] = mapped_column(String(240), nullable=True)
    role: Mapped[str] = mapped_column(String(200))
    channel_type: Mapped[str] = mapped_column(String(40))
    channel_value: Mapped[str] = mapped_column(Text)
    professional: Mapped[bool] = mapped_column(Boolean, default=True)
    public_evidence_verified: Mapped[bool] = mapped_column(Boolean, default=False)
    timezone: Mapped[str | None] = mapped_column(String(80), nullable=True)
    locale: Mapped[str] = mapped_column(String(20), default="en")
    organization: Mapped[Organization] = relationship()
    __table_args__ = (UniqueConstraint("organization_id", "channel_value"),)


class ContactSource(Base, TimestampMixin):
    __tablename__ = "contact_sources"
    id: Mapped[int] = mapped_column(primary_key=True)
    contact_id: Mapped[int] = mapped_column(ForeignKey("contacts.id", ondelete="CASCADE"))
    source_url: Mapped[str] = mapped_column(Text)
    source_type: Mapped[str] = mapped_column(String(80))
    supporting_excerpt: Mapped[str] = mapped_column(Text)
    retrieved_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    source_hash: Mapped[str] = mapped_column(String(64))
    contact: Mapped[Contact] = relationship()
    __table_args__ = (UniqueConstraint("contact_id", "source_url"),)


class OutreachPermissionEvidence(Base, TimestampMixin):
    __tablename__ = "outreach_permission_evidence"
    id: Mapped[int] = mapped_column(primary_key=True)
    contact_id: Mapped[int] = mapped_column(
        ForeignKey("contacts.id", ondelete="CASCADE"), unique=True
    )
    basis: Mapped[str] = mapped_column(String(120))
    recipient_entity_type: Mapped[str] = mapped_column(String(40), default="unknown")
    public_address: Mapped[bool] = mapped_column(Boolean, default=False)
    no_solicitation_notice: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    relevant_to_role: Mapped[bool] = mapped_column(Boolean, default=False)
    evidence_url: Mapped[str] = mapped_column(Text)
    evidence_excerpt: Mapped[str] = mapped_column(Text)
    reviewed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    contact: Mapped[Contact] = relationship()


class ProspectScore(Base, TimestampMixin):
    __tablename__ = "prospect_scores"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    total: Mapped[float] = mapped_column(Float)
    components: Mapped[dict[str, Any]] = mapped_column(JSON)
    explanation: Mapped[str] = mapped_column(Text)
    evidence_refs: Mapped[list[str]] = mapped_column(JSON)
    model_version: Mapped[str] = mapped_column(String(80), default="deterministic-v1")
    organization: Mapped[Organization] = relationship()
    __table_args__ = (UniqueConstraint("organization_id", "model_version"),)


class Offer(Base, TimestampMixin):
    __tablename__ = "offers"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    offer_type: Mapped[str] = mapped_column(String(80))
    problem: Mapped[str] = mapped_column(Text)
    proposition: Mapped[str] = mapped_column(Text)
    capability_ids: Mapped[list[str]] = mapped_column(JSON)
    evidence_refs: Mapped[list[str]] = mapped_column(JSON)
    limitations: Mapped[list[str]] = mapped_column(JSON)
    organization: Mapped[Organization] = relationship()
    __table_args__ = (UniqueConstraint("organization_id", "offer_type"),)


class Demo(Base, TimestampMixin):
    __tablename__ = "demos"
    id: Mapped[int] = mapped_column(primary_key=True)
    demo_id: Mapped[str] = mapped_column(String(120), unique=True)
    name: Mapped[str] = mapped_column(String(240))
    domains: Mapped[list[str]] = mapped_column(JSON)
    command_windows: Mapped[str] = mapped_column(Text)
    command_posix: Mapped[str] = mapped_column(Text)
    safety_mode: Mapped[str] = mapped_column(String(60))
    evidence_refs: Mapped[list[str]] = mapped_column(JSON)


class DemoPlan(Base, TimestampMixin):
    __tablename__ = "demo_plans"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    existing_demo_id: Mapped[int | None] = mapped_column(ForeignKey("demos.id"), nullable=True)
    disposition: Mapped[str] = mapped_column(String(80))
    plan: Mapped[dict[str, Any]] = mapped_column(JSON)
    engineering_hours: Mapped[int] = mapped_column(Integer, default=0)
    safe_to_auto_implement: Mapped[bool] = mapped_column(Boolean, default=False)
    organization: Mapped[Organization] = relationship()
    demo: Mapped[Demo | None] = relationship()
    __table_args__ = (UniqueConstraint("organization_id"),)


class GeneratedAsset(Base, TimestampMixin):
    __tablename__ = "generated_assets"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id", ondelete="CASCADE"))
    asset_type: Mapped[str] = mapped_column(String(80))
    persona: Mapped[str] = mapped_column(String(80))
    format: Mapped[str] = mapped_column(String(20))
    content: Mapped[str] = mapped_column(Text)
    evidence_refs: Mapped[list[str]] = mapped_column(JSON)
    content_hash: Mapped[str] = mapped_column(String(64), index=True)
    validated: Mapped[bool] = mapped_column(Boolean, default=False)
    organization: Mapped[Organization] = relationship()
    __table_args__ = (UniqueConstraint("organization_id", "asset_type", "persona", "format"),)


class Campaign(Base, TimestampMixin):
    __tablename__ = "campaigns"
    id: Mapped[int] = mapped_column(primary_key=True)
    campaign_key: Mapped[str] = mapped_column(String(120), unique=True)
    name: Mapped[str] = mapped_column(String(240))
    audience: Mapped[str] = mapped_column(String(80))
    status: Mapped[str] = mapped_column(String(40), default="ACTIVE")
    paused: Mapped[bool] = mapped_column(Boolean, default=False)
    config_snapshot: Mapped[dict[str, Any]] = mapped_column(JSON)


class CampaignVariant(Base, TimestampMixin):
    __tablename__ = "campaign_variants"
    id: Mapped[int] = mapped_column(primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id", ondelete="CASCADE"))
    key: Mapped[str] = mapped_column(String(80))
    subject_style: Mapped[str] = mapped_column(String(80))
    framing: Mapped[str] = mapped_column(String(80))
    call_to_action: Mapped[str] = mapped_column(String(160))
    campaign: Mapped[Campaign] = relationship()
    __table_args__ = (UniqueConstraint("campaign_id", "key"),)


class EmailThread(Base, TimestampMixin):
    __tablename__ = "email_threads"
    id: Mapped[int] = mapped_column(primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id"))
    contact_id: Mapped[int] = mapped_column(ForeignKey("contacts.id"))
    provider_thread_id: Mapped[str | None] = mapped_column(String(255), nullable=True, unique=True)
    subject: Mapped[str] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(80), default="QUEUED")
    sequence_paused: Mapped[bool] = mapped_column(Boolean, default=False)
    organization: Mapped[Organization] = relationship()
    contact: Mapped[Contact] = relationship()
    __table_args__ = (UniqueConstraint("campaign_id", "organization_id", "contact_id"),)


class EmailMessage(Base, TimestampMixin):
    __tablename__ = "email_messages"
    id: Mapped[int] = mapped_column(primary_key=True)
    thread_id: Mapped[int] = mapped_column(ForeignKey("email_threads.id", ondelete="CASCADE"))
    provider_message_id: Mapped[str | None] = mapped_column(String(255), nullable=True, unique=True)
    direction: Mapped[str] = mapped_column(String(20))
    sequence_number: Mapped[int] = mapped_column(Integer, default=0)
    subject: Mapped[str] = mapped_column(Text)
    body_text: Mapped[str] = mapped_column(Text)
    body_html: Mapped[str] = mapped_column(Text)
    status: Mapped[str] = mapped_column(String(80))
    sent_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    delivered_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    scheduled_for: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    idempotency_key: Mapped[str] = mapped_column(String(64), unique=True)
    thread: Mapped[EmailThread] = relationship()


class ReplyClassification(Base, TimestampMixin):
    __tablename__ = "reply_classifications"
    id: Mapped[int] = mapped_column(primary_key=True)
    message_id: Mapped[int] = mapped_column(
        ForeignKey("email_messages.id", ondelete="CASCADE"), unique=True
    )
    classification: Mapped[str] = mapped_column(String(80))
    confidence: Mapped[float] = mapped_column(Float)
    explanation: Mapped[str] = mapped_column(Text)
    deterministic: Mapped[bool] = mapped_column(Boolean)
    escalation_required: Mapped[bool] = mapped_column(Boolean, default=False)


class FollowUp(Base, TimestampMixin):
    __tablename__ = "followups"
    id: Mapped[int] = mapped_column(primary_key=True)
    thread_id: Mapped[int] = mapped_column(ForeignKey("email_threads.id", ondelete="CASCADE"))
    sequence_number: Mapped[int] = mapped_column(Integer)
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True))
    value_add_type: Mapped[str] = mapped_column(String(80))
    status: Mapped[str] = mapped_column(String(40), default="SCHEDULED")
    thread: Mapped[EmailThread] = relationship()
    __table_args__ = (UniqueConstraint("thread_id", "sequence_number"),)


class Meeting(Base, TimestampMixin):
    __tablename__ = "meetings"
    id: Mapped[int] = mapped_column(primary_key=True)
    thread_id: Mapped[int] = mapped_column(ForeignKey("email_threads.id", ondelete="CASCADE"))
    provider_event_id: Mapped[str | None] = mapped_column(String(255), nullable=True, unique=True)
    status: Mapped[str] = mapped_column(String(60), default="OPTIONS_PROPOSED")
    timezone: Mapped[str] = mapped_column(String(80))
    options: Mapped[list[dict[str, Any]]] = mapped_column(JSON)
    agreed_start: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    agreed_end: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    agenda: Mapped[str] = mapped_column(Text)
    recipient_agreed: Mapped[bool] = mapped_column(Boolean, default=False)


class ComplianceDecision(Base, TimestampMixin):
    __tablename__ = "compliance_decisions"
    id: Mapped[int] = mapped_column(primary_key=True)
    organization_id: Mapped[int] = mapped_column(ForeignKey("organizations.id"))
    contact_id: Mapped[int] = mapped_column(ForeignKey("contacts.id"))
    asset_id: Mapped[int] = mapped_column(ForeignKey("generated_assets.id"))
    decision: Mapped[str] = mapped_column(String(80))
    reasons: Mapped[list[str]] = mapped_column(JSON)
    policy_version: Mapped[str] = mapped_column(String(80), default="policy-v1")
    lawful_basis: Mapped[str | None] = mapped_column(String(120), nullable=True)
    source_refs: Mapped[list[str]] = mapped_column(JSON)
    __table_args__ = (UniqueConstraint("contact_id", "asset_id", "policy_version"),)


class SuppressionEntry(Base, TimestampMixin):
    __tablename__ = "suppression_entries"
    id: Mapped[int] = mapped_column(primary_key=True)
    scope: Mapped[str] = mapped_column(String(40))
    normalized_value: Mapped[str] = mapped_column(String(320), index=True)
    reason: Mapped[str] = mapped_column(String(160))
    source: Mapped[str] = mapped_column(String(120))
    active: Mapped[bool] = mapped_column(Boolean, default=True)
    __table_args__ = (UniqueConstraint("scope", "normalized_value"),)


class Experiment(Base, TimestampMixin):
    __tablename__ = "experiments"
    id: Mapped[int] = mapped_column(primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    hypothesis: Mapped[str] = mapped_column(Text)
    variables: Mapped[list[str]] = mapped_column(JSON)
    variants: Mapped[list[dict[str, Any]]] = mapped_column(JSON)
    assignment: Mapped[dict[str, str]] = mapped_column(JSON, default=dict)
    results: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)
    stopping_rule: Mapped[str] = mapped_column(Text)
    minimum_sample_size: Mapped[int] = mapped_column(Integer, default=30)
    selected_result: Mapped[str | None] = mapped_column(String(120), nullable=True)
    status: Mapped[str] = mapped_column(String(40), default="DRAFT")


class MetricSnapshot(Base, TimestampMixin):
    __tablename__ = "metric_snapshots"
    id: Mapped[int] = mapped_column(primary_key=True)
    campaign_id: Mapped[int] = mapped_column(ForeignKey("campaigns.id"))
    captured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    metrics: Mapped[dict[str, Any]] = mapped_column(JSON)
    dimensions: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)


class WorkflowState(Base, TimestampMixin):
    __tablename__ = "workflow_states"
    id: Mapped[int] = mapped_column(primary_key=True)
    entity_type: Mapped[str] = mapped_column(String(80))
    entity_id: Mapped[str] = mapped_column(String(160))
    state: Mapped[str] = mapped_column(String(80))
    version: Mapped[int] = mapped_column(Integer, default=1)
    __table_args__ = (UniqueConstraint("entity_type", "entity_id"),)


class AuditEvent(Base):
    __tablename__ = "audit_events"
    id: Mapped[int] = mapped_column(primary_key=True)
    event_id: Mapped[str] = mapped_column(String(64), unique=True)
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    actor: Mapped[str] = mapped_column(String(120))
    action: Mapped[str] = mapped_column(String(160))
    entity_type: Mapped[str] = mapped_column(String(80))
    entity_id: Mapped[str] = mapped_column(String(160))
    from_state: Mapped[str | None] = mapped_column(String(80), nullable=True)
    to_state: Mapped[str | None] = mapped_column(String(80), nullable=True)
    reason: Mapped[str] = mapped_column(Text)
    source: Mapped[str] = mapped_column(Text)
    reversible: Mapped[bool] = mapped_column(Boolean, default=False)
    details: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)


class ProviderFailure(Base):
    __tablename__ = "provider_failures"
    id: Mapped[int] = mapped_column(primary_key=True)
    occurred_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    provider: Mapped[str] = mapped_column(String(120))
    operation: Mapped[str] = mapped_column(String(160))
    error_type: Mapped[str] = mapped_column(String(160))
    redacted_message: Mapped[str] = mapped_column(Text)
    retryable: Mapped[bool] = mapped_column(Boolean, default=True)
    resolved: Mapped[bool] = mapped_column(Boolean, default=False)


class JobRun(Base):
    __tablename__ = "job_runs"
    id: Mapped[int] = mapped_column(primary_key=True)
    job_key: Mapped[str] = mapped_column(String(120), index=True)
    idempotency_key: Mapped[str] = mapped_column(String(160), unique=True)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    status: Mapped[str] = mapped_column(String(40), default="RUNNING")
    attempts: Mapped[int] = mapped_column(Integer, default=1)
    details: Mapped[dict[str, Any]] = mapped_column(JSON, default=dict)


class PauseControl(Base, TimestampMixin):
    __tablename__ = "pause_controls"
    id: Mapped[int] = mapped_column(primary_key=True)
    scope: Mapped[str] = mapped_column(String(40))
    value: Mapped[str] = mapped_column(String(240))
    paused: Mapped[bool] = mapped_column(Boolean, default=True)
    reason: Mapped[str] = mapped_column(Text, default="Paused from local dashboard")
    __table_args__ = (UniqueConstraint("scope", "value"),)
