from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any, Literal

import yaml
from pydantic import BaseModel, Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

CAMPAIGN_ROOT = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    """Environment configuration. External writes require two independent switches."""

    model_config = SettingsConfigDict(
        env_file=".env", env_prefix="", case_sensitive=False, extra="ignore"
    )

    campaign_mode: Literal["DRY_RUN", "LIVE"] = "DRY_RUN"
    campaign_live_send: bool = False
    campaign_config_dir: Path = CAMPAIGN_ROOT / "config"
    campaign_database_url: str = f"sqlite:///{(CAMPAIGN_ROOT / 'campaign.db').as_posix()}"
    campaign_repository_root: Path = CAMPAIGN_ROOT.parent
    campaign_report_dir: Path = CAMPAIGN_ROOT / "reports"
    campaign_log_level: str = "INFO"
    google_oauth_client_file: Path = Path("secrets/google-oauth-client.json")
    google_oauth_token_file: Path = Path("secrets/google-token.enc")
    google_oauth_account: str = ""
    search_provider: str = "fixture"
    search_verified_file: Path = CAMPAIGN_ROOT / "config" / "verified-prospects.yml"
    search_api_endpoint: str = ""
    search_api_key: str = ""
    llm_provider: str = "deterministic"
    llm_api_endpoint: str = ""
    llm_api_key: str = ""
    llm_model: str = ""
    campaign_sender_name: str = ""
    campaign_sender_email: str = ""
    campaign_organization: str = "Jneopallium"
    campaign_postal_address: str = ""
    campaign_reply_to: str = ""
    campaign_escalation_email: str = ""
    campaign_default_timezone: str = "Europe/Kyiv"
    campaign_profile_file: Path | None = None

    @property
    def live_writes_enabled(self) -> bool:
        return self.campaign_mode == "LIVE" and self.campaign_live_send

    @field_validator(
        "campaign_repository_root",
        "campaign_config_dir",
        "campaign_report_dir",
        "search_verified_file",
        "campaign_profile_file",
    )
    @classmethod
    def expand_path(cls, value: Path | None) -> Path | None:
        return value.expanduser() if value is not None else None


class LimitsConfig(BaseModel):
    max_new_contacts_per_day: int = Field(default=10, ge=0, le=100)
    max_outbound_per_day: int = Field(default=20, ge=0, le=200)
    max_initial_contacts_per_organization: int = Field(default=1, ge=1, le=3)
    max_automated_followups: int = Field(default=2, ge=0, le=4)
    min_business_days_between_followups: int = Field(default=4, ge=2, le=30)
    bounce_halt_rate: float = Field(default=0.05, ge=0, le=1)
    complaint_halt_count: int = Field(default=1, ge=0)


class CampaignPolicy(BaseModel):
    campaign_id: str = "dry-run-pilot"
    language: str = "en"
    target_regions: list[str] = Field(default_factory=lambda: ["EU", "UK", "US", "CA"])
    excluded_regions: list[str] = Field(default_factory=list)
    prospect_types: list[str] = Field(default_factory=list)
    active_domains: list[str] = Field(default_factory=list)
    excluded_domains: list[str] = Field(default_factory=list)
    minimum_prospect_score: float = 60.0
    demo_auto_plan_threshold: float = 72.0
    research_budget_usd: float = 20.0
    llm_budget_usd: float = 20.0
    demo_engineering_budget_hours: int = 40
    send_window_start: int = Field(default=9, ge=0, le=23)
    send_window_end: int = Field(default=16, ge=1, le=24)
    limits: LimitsConfig = Field(default_factory=LimitsConfig)


class CompliancePolicy(BaseModel):
    allowed_organization_types: list[str]
    require_public_contact_evidence: bool = True
    require_source_and_lawful_basis: bool = True
    require_postal_address: bool = True
    require_opt_out: bool = True
    personal_email_domains: list[str] = Field(default_factory=list)
    geographic_exclusions: list[str] = Field(default_factory=list)
    legal_review_regions: list[str] = Field(default_factory=list)
    prohibited_claim_patterns: list[str] = Field(default_factory=list)


def load_yaml(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as stream:
        data = yaml.safe_load(stream) or {}
    if not isinstance(data, dict):
        raise ValueError(f"Expected a YAML mapping in {path}")
    return data


def merge_config(base: dict[str, Any], override: dict[str, Any]) -> dict[str, Any]:
    """Recursively apply a small deployment profile without changing safe defaults."""
    merged = dict(base)
    for key, value in override.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = merge_config(merged[key], value)
        else:
            merged[key] = value
    return merged


@dataclass(frozen=True)
class AppConfig:
    settings: Settings
    campaign: CampaignPolicy
    compliance: CompliancePolicy
    raw_domains: dict[str, Any]
    raw_offers: dict[str, Any]
    raw_demo_matching: dict[str, Any]


def load_config(settings: Settings | None = None) -> AppConfig:
    settings = settings or Settings()
    base = settings.campaign_config_dir
    campaigns = load_yaml(base / "campaigns.yml")
    campaign_defaults = campaigns.get("default", {})
    if settings.campaign_profile_file is not None:
        profile = load_yaml(settings.campaign_profile_file)
        campaign_defaults = merge_config(campaign_defaults, profile.get("default", profile))
    policies = load_yaml(base / "compliance.yml")
    return AppConfig(
        settings=settings,
        campaign=CampaignPolicy.model_validate(campaign_defaults),
        compliance=CompliancePolicy.model_validate(policies),
        raw_domains=load_yaml(base / "domains.yml"),
        raw_offers=load_yaml(base / "offers.yml"),
        raw_demo_matching=load_yaml(base / "demo-matching.yml"),
    )
