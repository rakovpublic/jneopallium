from __future__ import annotations

from pathlib import Path

import httpx
import yaml

from jneo_campaign.providers.interfaces import SearchFact
from jneo_campaign.security import sanitize_external_content, validate_outbound_url


class FixtureSearchProvider:
    """Recorded official-site findings for deterministic/offline acceptance runs."""

    name = "fixture"

    def __init__(self, fixture_path: Path) -> None:
        self.fixture_path = fixture_path

    def discover(self, domains: list[str], limit: int) -> list[SearchFact]:
        with self.fixture_path.open("r", encoding="utf-8") as stream:
            data = yaml.safe_load(stream) or {}
        allowed = {item.lower() for item in domains}
        facts: list[SearchFact] = []
        for item in data.get("organizations", []):
            if allowed and item["target_domain"].lower() not in allowed:
                continue
            for key in ("source_url", "contact_source_url"):
                if item.get(key):
                    validate_outbound_url(item[key])
            excerpt = sanitize_external_content(item["supporting_excerpt"])
            contact_excerpt = sanitize_external_content(item.get("contact_supporting_excerpt", ""))
            if excerpt.prompt_injection_suspected or contact_excerpt.prompt_injection_suspected:
                continue
            facts.append(SearchFact(**item))
            if len(facts) >= limit:
                break
        return facts


class JsonSearchProvider:
    """Adapter for a configured, authorized JSON search/research API.

    The endpoint must return ``{"results": [SearchFact-compatible objects]}``. This adapter
    never crawls result pages; source collection policy remains with the configured provider.
    """

    name = "json-api"

    def __init__(self, endpoint: str, api_key: str, timeout_seconds: float = 20) -> None:
        self.endpoint = validate_outbound_url(endpoint)
        self.api_key = api_key
        self.timeout_seconds = timeout_seconds

    def discover(self, domains: list[str], limit: int) -> list[SearchFact]:
        response = httpx.post(
            self.endpoint,
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={
                "domains": domains,
                "limit": limit,
                "allowed_sources": [
                    "official_website",
                    "official_contact_page",
                    "public_grant_list",
                    "public_conference_list",
                    "official_github_organization",
                ],
                "forbidden": [
                    "private_social_network",
                    "guessed_email",
                    "authentication_bypass",
                    "captcha_bypass",
                ],
            },
            timeout=self.timeout_seconds,
        )
        response.raise_for_status()
        payload = response.json()
        result: list[SearchFact] = []
        for row in payload.get("results", []):
            validate_outbound_url(row["source_url"])
            excerpt = sanitize_external_content(row["supporting_excerpt"])
            if excerpt.prompt_injection_suspected:
                continue
            result.append(SearchFact(**row))
        return result[:limit]
