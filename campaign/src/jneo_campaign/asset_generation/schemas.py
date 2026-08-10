from __future__ import annotations

from pydantic import BaseModel, Field, field_validator


class PropositionOutput(BaseModel):
    organization: str
    persona: str
    subject_line: str = Field(min_length=8, max_length=100)
    specific_activity: str
    activity_evidence: str
    relevant_capability: str
    implemented_now: str
    proposed_work: str
    next_step: str
    poc_inputs: list[str]
    poc_outputs: list[str]
    limitations: list[str]
    scope_boundary: str
    call_to_action: str
    evidence_refs: list[str] = Field(min_length=2)

    @field_validator("implemented_now")
    @classmethod
    def block_unsupported_readiness_claims(cls, value: str) -> str:
        prohibited = ("production ready", "clinically proven", "guaranteed", "accredited")
        if any(term in value.lower() for term in prohibited):
            raise ValueError("Unsupported readiness claim")
        return value

    @field_validator("activity_evidence", "scope_boundary", "call_to_action")
    @classmethod
    def block_internal_authoring_markers(cls, value: str) -> str:
        prohibited = ("[source:", "<untrusted_external_data>", "scope limits:")
        if any(term in value.lower() for term in prohibited):
            raise ValueError("Internal authoring markers must not appear in outreach copy")
        return value
