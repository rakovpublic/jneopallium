from __future__ import annotations

from jneo_campaign.config import CAMPAIGN_ROOT, load_config


def test_campaign_profile_overlays_nested_limits(settings, tmp_path) -> None:
    profile = tmp_path / "profile.yml"
    profile.write_text(
        """\
default:
  campaign_id: live-profile
  target_regions: [US]
  limits:
    max_new_contacts_per_day: 3
""",
        encoding="utf-8",
    )

    config = load_config(settings.model_copy(update={"campaign_profile_file": profile}))

    assert config.campaign.campaign_id == "live-profile"
    assert config.campaign.target_regions == ["US"]
    assert config.campaign.limits.max_new_contacts_per_day == 3
    assert config.campaign.limits.max_outbound_per_day == 20


def test_live_profile_targets_requested_regions(settings) -> None:
    config = load_config(
        settings.model_copy(
            update={"campaign_profile_file": CAMPAIGN_ROOT / "config" / "live-pilot.yml"}
        )
    )

    assert config.campaign.target_regions == [
        "US",
        "EU",
        "CA",
        "JP",
        "KR",
        "UA",
        "IL",
        "GB",
        "AU",
    ]
