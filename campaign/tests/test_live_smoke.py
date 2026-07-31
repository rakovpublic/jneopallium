from __future__ import annotations

import os

import pytest

from jneo_campaign.config import load_config
from jneo_campaign.providers.factory import build_providers


@pytest.mark.live
@pytest.mark.skipif(
    os.getenv("JNEO_ENABLE_LIVE_TESTS", "false").lower() != "true",
    reason="Set JNEO_ENABLE_LIVE_TESTS=true with dedicated test credentials",
)
def test_live_credentials_read_only_smoke() -> None:
    """Credential/profile check only; this test never sends mail or creates an event."""
    config = load_config()
    assert config.settings.live_writes_enabled
    providers = build_providers(config)
    assert providers.gmail.validate_credentials()["valid"]
    assert providers.calendar.validate_credentials()["valid"]
