from __future__ import annotations

from datetime import UTC, date, datetime, time
from pathlib import Path
from typing import Any

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
        default_retrieved_at = data.get("retrieved_at")
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
            row = dict(item)
            retrieved_at = _parse_retrieved_at(row.pop("retrieved_at", default_retrieved_at))
            facts.append(SearchFact(**row, retrieved_at=retrieved_at))
            if len(facts) >= limit:
                break
        return facts


class VerifiedFileSearchProvider(FixtureSearchProvider):
    """Operator-reviewed public facts with an explicit evidence timestamp.

    Unlike the deterministic fixture, this provider may be selected for a LIVE run. The file is
    still treated as untrusted input and passes through the same URL and content controls.
    """

    name = "verified-file"


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
            for key in ("source_url", "contact_source_url"):
                if row.get(key):
                    validate_outbound_url(row[key])
            excerpt = sanitize_external_content(row["supporting_excerpt"])
            if excerpt.prompt_injection_suspected:
                continue
            result.append(
                SearchFact(
                    **{key: value for key, value in row.items() if key != "retrieved_at"},
                    retrieved_at=_parse_retrieved_at(row.get("retrieved_at")),
                )
            )
        return result[:limit]


def _parse_retrieved_at(value: Any) -> datetime:
    if isinstance(value, datetime):
        parsed = value
    elif isinstance(value, date):
        parsed = datetime.combine(value, time.min)
    elif isinstance(value, str) and value.strip():
        parsed = datetime.fromisoformat(value.strip().replace("Z", "+00:00"))
    else:
        raise ValueError("Prospect evidence requires an explicit retrieved_at timestamp")
    return parsed.replace(tzinfo=UTC) if parsed.tzinfo is None else parsed.astimezone(UTC)
