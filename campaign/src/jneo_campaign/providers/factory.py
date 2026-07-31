from __future__ import annotations

from dataclasses import dataclass

from jneo_campaign.config import CAMPAIGN_ROOT, AppConfig
from jneo_campaign.providers.credentials import EncryptedCredentialStore
from jneo_campaign.providers.google import (
    GoogleCalendarProvider,
    GoogleCredentialManager,
    GoogleGmailProvider,
)
from jneo_campaign.providers.interfaces import (
    CalendarProvider,
    GmailProvider,
    SearchProvider,
    StructuredLLMProvider,
)
from jneo_campaign.providers.llm import DeterministicLLMProvider, JsonLLMProvider
from jneo_campaign.providers.mock import MockCalendarProvider, MockGmailProvider
from jneo_campaign.providers.search import FixtureSearchProvider, JsonSearchProvider


@dataclass
class Providers:
    gmail: GmailProvider
    calendar: CalendarProvider
    search: SearchProvider
    llm: StructuredLLMProvider


def build_providers(config: AppConfig) -> Providers:
    settings = config.settings
    if settings.live_writes_enabled:
        store = EncryptedCredentialStore(
            settings.google_oauth_token_file, settings.google_oauth_account
        )
        manager = GoogleCredentialManager(str(settings.google_oauth_client_file), store)
        gmail: GmailProvider = GoogleGmailProvider(manager, settings.campaign_sender_email)
        calendar: CalendarProvider = GoogleCalendarProvider(manager)
    else:
        gmail = MockGmailProvider()
        calendar = MockCalendarProvider()
    if settings.search_provider == "fixture":
        search: SearchProvider = FixtureSearchProvider(CAMPAIGN_ROOT / "fixtures" / "prospects.yml")
    else:
        search = JsonSearchProvider(settings.search_api_endpoint, settings.search_api_key)
    if settings.llm_provider == "deterministic":
        llm: StructuredLLMProvider = DeterministicLLMProvider()
    else:
        llm = JsonLLMProvider(settings.llm_api_endpoint, settings.llm_api_key, settings.llm_model)
    return Providers(gmail=gmail, calendar=calendar, search=search, llm=llm)
