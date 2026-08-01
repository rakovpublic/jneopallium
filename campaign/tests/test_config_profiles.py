from __future__ import annotations

from jneo_campaign.config import load_config


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
