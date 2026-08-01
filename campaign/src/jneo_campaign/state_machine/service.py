from __future__ import annotations

import hashlib
from enum import StrEnum

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.storage.models import AuditEvent, WorkflowState


class CampaignState(StrEnum):
    DOMAIN_DISCOVERED = "DOMAIN_DISCOVERED"
    DOMAIN_RESEARCHED = "DOMAIN_RESEARCHED"
    DOMAIN_SCORED = "DOMAIN_SCORED"
    ORGANIZATION_DISCOVERED = "ORGANIZATION_DISCOVERED"
    ORGANIZATION_VERIFIED = "ORGANIZATION_VERIFIED"
    CONTACT_DISCOVERED = "CONTACT_DISCOVERED"
    CONTACT_VERIFIED = "CONTACT_VERIFIED"
    PROSPECT_SCORED = "PROSPECT_SCORED"
    OFFER_SELECTED = "OFFER_SELECTED"
    DEMO_SELECTED = "DEMO_SELECTED"
    DEMO_SPEC_REQUIRED = "DEMO_SPEC_REQUIRED"
    MATERIALS_GENERATED = "MATERIALS_GENERATED"
    COMPLIANCE_APPROVED = "COMPLIANCE_APPROVED"
    QUEUED = "QUEUED"
    CONTACTED = "CONTACTED"
    DELIVERED = "DELIVERED"
    BOUNCED = "BOUNCED"
    REPLIED = "REPLIED"
    POSITIVE_REPLY = "POSITIVE_REPLY"
    QUESTION_RECEIVED = "QUESTION_RECEIVED"
    MEETING_REQUESTED = "MEETING_REQUESTED"
    MEETING_SCHEDULED = "MEETING_SCHEDULED"
    PROPOSAL_REQUESTED = "PROPOSAL_REQUESTED"
    PROPOSAL_SENT = "PROPOSAL_SENT"
    POC_REQUESTED = "POC_REQUESTED"
    DO_NOT_CONTACT = "DO_NOT_CONTACT"
    NOT_INTERESTED = "NOT_INTERESTED"
    DISQUALIFIED = "DISQUALIFIED"
    WON = "WON"
    LOST = "LOST"


TERMINAL = {
    CampaignState.DO_NOT_CONTACT,
    CampaignState.NOT_INTERESTED,
    CampaignState.DISQUALIFIED,
    CampaignState.WON,
    CampaignState.LOST,
}

PRE_OUTREACH_RANK: dict[CampaignState, int] = {
    CampaignState.ORGANIZATION_DISCOVERED: 0,
    CampaignState.ORGANIZATION_VERIFIED: 1,
    CampaignState.CONTACT_DISCOVERED: 2,
    CampaignState.CONTACT_VERIFIED: 3,
    CampaignState.PROSPECT_SCORED: 4,
    CampaignState.OFFER_SELECTED: 5,
    CampaignState.DEMO_SELECTED: 6,
    CampaignState.DEMO_SPEC_REQUIRED: 6,
    CampaignState.MATERIALS_GENERATED: 7,
    CampaignState.COMPLIANCE_APPROVED: 8,
    CampaignState.QUEUED: 9,
    CampaignState.CONTACTED: 10,
    CampaignState.DELIVERED: 11,
}

DOMAIN_RANK: dict[CampaignState, int] = {
    CampaignState.DOMAIN_DISCOVERED: 0,
    CampaignState.DOMAIN_RESEARCHED: 1,
    CampaignState.DOMAIN_SCORED: 2,
}

# Entity-specific workflows can start at their first applicable state. The graph keeps
# outreach transitions strict while allowing reply outcomes to branch independently.
ALLOWED_TRANSITIONS: dict[CampaignState, set[CampaignState]] = {
    CampaignState.DOMAIN_DISCOVERED: {CampaignState.DOMAIN_RESEARCHED},
    CampaignState.DOMAIN_RESEARCHED: {CampaignState.DOMAIN_SCORED},
    CampaignState.ORGANIZATION_DISCOVERED: {
        CampaignState.ORGANIZATION_VERIFIED,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.ORGANIZATION_VERIFIED: {
        CampaignState.CONTACT_DISCOVERED,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.CONTACT_DISCOVERED: {
        CampaignState.CONTACT_VERIFIED,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.CONTACT_VERIFIED: {
        CampaignState.PROSPECT_SCORED,
        CampaignState.DO_NOT_CONTACT,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.PROSPECT_SCORED: {
        CampaignState.OFFER_SELECTED,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.OFFER_SELECTED: {
        CampaignState.DEMO_SELECTED,
        CampaignState.DEMO_SPEC_REQUIRED,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.DEMO_SELECTED: {CampaignState.MATERIALS_GENERATED},
    CampaignState.DEMO_SPEC_REQUIRED: {CampaignState.MATERIALS_GENERATED},
    CampaignState.MATERIALS_GENERATED: {
        CampaignState.COMPLIANCE_APPROVED,
        CampaignState.DO_NOT_CONTACT,
        CampaignState.DISQUALIFIED,
    },
    CampaignState.COMPLIANCE_APPROVED: {
        CampaignState.QUEUED,
        CampaignState.DO_NOT_CONTACT,
    },
    CampaignState.QUEUED: {CampaignState.CONTACTED, CampaignState.DO_NOT_CONTACT},
    CampaignState.CONTACTED: {
        CampaignState.DELIVERED,
        CampaignState.BOUNCED,
        CampaignState.REPLIED,
        CampaignState.DO_NOT_CONTACT,
    },
    CampaignState.DELIVERED: {
        CampaignState.REPLIED,
        CampaignState.BOUNCED,
        CampaignState.DO_NOT_CONTACT,
        CampaignState.LOST,
    },
    CampaignState.REPLIED: {
        CampaignState.POSITIVE_REPLY,
        CampaignState.QUESTION_RECEIVED,
        CampaignState.MEETING_REQUESTED,
        CampaignState.PROPOSAL_REQUESTED,
        CampaignState.POC_REQUESTED,
        CampaignState.DO_NOT_CONTACT,
        CampaignState.NOT_INTERESTED,
    },
    CampaignState.POSITIVE_REPLY: {
        CampaignState.MEETING_REQUESTED,
        CampaignState.PROPOSAL_REQUESTED,
        CampaignState.POC_REQUESTED,
        CampaignState.WON,
        CampaignState.LOST,
    },
    CampaignState.QUESTION_RECEIVED: {
        CampaignState.POSITIVE_REPLY,
        CampaignState.MEETING_REQUESTED,
        CampaignState.NOT_INTERESTED,
    },
    CampaignState.MEETING_REQUESTED: {
        CampaignState.MEETING_SCHEDULED,
        CampaignState.NOT_INTERESTED,
    },
    CampaignState.MEETING_SCHEDULED: {
        CampaignState.PROPOSAL_REQUESTED,
        CampaignState.POC_REQUESTED,
        CampaignState.WON,
        CampaignState.LOST,
    },
    CampaignState.PROPOSAL_REQUESTED: {CampaignState.PROPOSAL_SENT},
    CampaignState.PROPOSAL_SENT: {
        CampaignState.POC_REQUESTED,
        CampaignState.WON,
        CampaignState.LOST,
    },
    CampaignState.POC_REQUESTED: {CampaignState.WON, CampaignState.LOST},
}


class InvalidTransition(ValueError):
    pass


def transition(
    session: Session,
    *,
    entity_type: str,
    entity_id: str | int,
    to_state: CampaignState,
    reason: str,
    source: str,
    actor: str = "system",
    initial_state: CampaignState | None = None,
    reversible: bool = False,
) -> WorkflowState:
    """Persist an idempotent, attributable state transition and its audit event."""
    key = str(entity_id)
    current = session.scalar(
        select(WorkflowState).where(
            WorkflowState.entity_type == entity_type, WorkflowState.entity_id == key
        )
    )
    if current is None:
        state = initial_state or to_state
        current = WorkflowState(entity_type=entity_type, entity_id=key, state=state.value)
        session.add(current)
        session.flush()
        _audit(session, current, None, state, reason, source, actor, reversible)
        if state == to_state:
            return current

    old = CampaignState(current.state)
    if old == to_state:
        return current
    if old in TERMINAL:
        raise InvalidTransition(f"{old.value} is terminal")
    if old in DOMAIN_RANK and to_state in DOMAIN_RANK and DOMAIN_RANK[old] >= DOMAIN_RANK[to_state]:
        return current
    if to_state in PRE_OUTREACH_RANK and (
        (old in PRE_OUTREACH_RANK and PRE_OUTREACH_RANK[old] >= PRE_OUTREACH_RANK[to_state])
        or old
        in {
            CampaignState.REPLIED,
            CampaignState.POSITIVE_REPLY,
            CampaignState.QUESTION_RECEIVED,
            CampaignState.MEETING_REQUESTED,
            CampaignState.MEETING_SCHEDULED,
            CampaignState.PROPOSAL_REQUESTED,
            CampaignState.PROPOSAL_SENT,
            CampaignState.POC_REQUESTED,
        }
    ):
        return current
    if to_state not in ALLOWED_TRANSITIONS.get(old, set()):
        raise InvalidTransition(f"Transition {old.value} -> {to_state.value} is not allowed")
    current.state = to_state.value
    current.version += 1
    _audit(session, current, old, to_state, reason, source, actor, reversible)
    return current


def _audit(
    session: Session,
    current: WorkflowState,
    old: CampaignState | None,
    new: CampaignState,
    reason: str,
    source: str,
    actor: str,
    reversible: bool,
) -> None:
    raw = f"{current.entity_type}|{current.entity_id}|{current.version}|{old}|{new}|{reason}"
    event_id = hashlib.sha256(raw.encode()).hexdigest()
    session.add(
        AuditEvent(
            event_id=event_id,
            actor=actor,
            action="STATE_TRANSITION",
            entity_type=current.entity_type,
            entity_id=current.entity_id,
            from_state=old.value if old else None,
            to_state=new.value,
            reason=reason,
            source=source,
            reversible=reversible,
            details={"workflow_version": current.version},
        )
    )
