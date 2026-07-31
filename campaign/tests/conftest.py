from __future__ import annotations

from pathlib import Path

import pytest

from jneo_campaign.config import CAMPAIGN_ROOT, Settings, load_config
from jneo_campaign.orchestrator import CampaignOrchestrator

REPOSITORY_ROOT = CAMPAIGN_ROOT.parent


@pytest.fixture
def settings(tmp_path: Path) -> Settings:
    return Settings(
        _env_file=None,
        campaign_mode="DRY_RUN",
        campaign_live_send=False,
        campaign_config_dir=CAMPAIGN_ROOT / "config",
        campaign_database_url=f"sqlite:///{(tmp_path / 'campaign.db').as_posix()}",
        campaign_repository_root=REPOSITORY_ROOT,
        campaign_report_dir=tmp_path / "reports",
        campaign_default_timezone="Europe/Kyiv",
    )


@pytest.fixture
def runner(settings: Settings) -> CampaignOrchestrator:
    return CampaignOrchestrator(config=load_config(settings))
