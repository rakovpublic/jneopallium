from __future__ import annotations

import base64
from datetime import datetime
from email.message import EmailMessage
from typing import Any

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build

from jneo_campaign.providers.credentials import EncryptedCredentialStore
from jneo_campaign.providers.interfaces import CalendarSlot, OutboundEmail, ProviderMessage

GMAIL_SCOPES = [
    "https://www.googleapis.com/auth/gmail.modify",
    "https://www.googleapis.com/auth/gmail.send",
]
CALENDAR_SCOPES = [
    "https://www.googleapis.com/auth/calendar.events",
    "https://www.googleapis.com/auth/calendar.freebusy",
]
GOOGLE_SCOPES = sorted(set(GMAIL_SCOPES + CALENDAR_SCOPES))


class GoogleCredentialManager:
    def __init__(self, client_file: str, store: EncryptedCredentialStore) -> None:
        self.client_file = client_file
        self.store = store

    def authorize(self, scopes: list[str] | None = None) -> Credentials:
        flow = InstalledAppFlow.from_client_secrets_file(self.client_file, scopes or GOOGLE_SCOPES)
        credentials = flow.run_local_server(port=0, access_type="offline", prompt="consent")
        self.store.save(self._to_payload(credentials))
        return credentials

    def credentials(self, required_scopes: list[str]) -> Credentials:
        payload = self.store.load()
        credentials = Credentials.from_authorized_user_info(payload, scopes=required_scopes)
        if credentials.expired and credentials.refresh_token:
            credentials.refresh(Request())
            self.store.save(self._to_payload(credentials))
        if not credentials.valid:
            raise RuntimeError("Google credentials are invalid; run `jneo-campaign auth gmail`")
        granted = set(credentials.scopes or [])
        missing = set(required_scopes) - granted
        if missing:
            raise RuntimeError(f"Google credential is missing required scopes: {sorted(missing)}")
        return credentials

    @staticmethod
    def _to_payload(credentials: Credentials) -> dict[str, object]:
        return {
            "token": credentials.token,
            "refresh_token": credentials.refresh_token,
            "token_uri": credentials.token_uri,
            "client_id": credentials.client_id,
            "client_secret": credentials.client_secret,
            "scopes": list(credentials.scopes or []),
        }


class GoogleGmailProvider:
    def __init__(self, manager: GoogleCredentialManager, sender: str) -> None:
        self.manager = manager
        self.sender = sender

    def _service(self):
        return build(
            "gmail", "v1", credentials=self.manager.credentials(GMAIL_SCOPES), cache_discovery=False
        )

    def validate_credentials(self) -> dict[str, Any]:
        profile = self._service().users().getProfile(userId="me").execute()
        return {"provider": "gmail", "valid": True, "email": profile.get("emailAddress")}

    def ensure_labels(self, labels: list[str]) -> dict[str, str]:
        service = self._service()
        existing = {
            item["name"]: item["id"]
            for item in service.users().labels().list(userId="me").execute().get("labels", [])
        }
        for name in labels:
            if name not in existing:
                item = (
                    service.users()
                    .labels()
                    .create(
                        userId="me",
                        body={
                            "name": name,
                            "labelListVisibility": "labelShow",
                            "messageListVisibility": "show",
                        },
                    )
                    .execute()
                )
                existing[name] = item["id"]
        return {name: existing[name] for name in labels}

    def send(self, message: OutboundEmail, labels: list[str]) -> tuple[str, str]:
        service = self._service()
        mime = EmailMessage()
        mime["To"] = message.to
        mime["From"] = self.sender
        mime["Reply-To"] = message.reply_to
        mime["Subject"] = message.subject
        mime["X-Jneopallium-Campaign-ID"] = message.idempotency_key
        for name, value in message.headers.items():
            mime[name] = value
        mime.set_content(message.body_text)
        mime.add_alternative(message.body_html, subtype="html")
        body: dict[str, Any] = {
            "raw": base64.urlsafe_b64encode(mime.as_bytes()).decode().rstrip("=")
        }
        if message.thread_id:
            body["threadId"] = message.thread_id
        result = service.users().messages().send(userId="me", body=body).execute()
        label_ids = list(self.ensure_labels(labels).values())
        service.users().messages().modify(
            userId="me", id=result["id"], body={"addLabelIds": label_ids}
        ).execute()
        return result["id"], result["threadId"]

    def sync(self, query: str) -> list[ProviderMessage]:
        service = self._service()
        response = service.users().messages().list(userId="me", q=query, maxResults=100).execute()
        result: list[ProviderMessage] = []
        for pointer in response.get("messages", []):
            item = (
                service.users()
                .messages()
                .get(userId="me", id=pointer["id"], format="full")
                .execute()
            )
            headers = {
                row["name"].lower(): row["value"]
                for row in item.get("payload", {}).get("headers", [])
            }
            body = _gmail_body_text(item.get("payload", {}))
            result.append(
                ProviderMessage(
                    message_id=item["id"],
                    thread_id=item["threadId"],
                    from_address=headers.get("from", ""),
                    to_address=headers.get("to", ""),
                    subject=headers.get("subject", ""),
                    body_text=body,
                    received_at=datetime.fromtimestamp(
                        int(item["internalDate"]) / 1000
                    ).astimezone(),
                    labels=tuple(item.get("labelIds", [])),
                )
            )
        return result


def _gmail_body_text(payload: dict[str, Any]) -> str:
    if payload.get("mimeType") == "text/plain" and payload.get("body", {}).get("data"):
        data = payload["body"]["data"]
        return base64.urlsafe_b64decode(data + "=" * (-len(data) % 4)).decode(errors="replace")
    for part in payload.get("parts", []):
        body = _gmail_body_text(part)
        if body:
            return body
    return ""


class GoogleCalendarProvider:
    def __init__(self, manager: GoogleCredentialManager) -> None:
        self.manager = manager

    def _service(self):
        return build(
            "calendar",
            "v3",
            credentials=self.manager.credentials(CALENDAR_SCOPES),
            cache_discovery=False,
        )

    def validate_credentials(self) -> dict[str, Any]:
        calendar = self._service().calendars().get(calendarId="primary").execute()
        return {"provider": "calendar", "valid": True, "summary": calendar.get("summary")}

    def free_busy(self, start: datetime, end: datetime, calendar_id: str) -> list[CalendarSlot]:
        response = (
            self._service()
            .freebusy()
            .query(
                body={
                    "timeMin": start.isoformat(),
                    "timeMax": end.isoformat(),
                    "items": [{"id": calendar_id}],
                }
            )
            .execute()
        )
        return [
            CalendarSlot(
                start=datetime.fromisoformat(row["start"]), end=datetime.fromisoformat(row["end"])
            )
            for row in response["calendars"][calendar_id].get("busy", [])
        ]

    def create_event(
        self,
        *,
        summary: str,
        description: str,
        start: datetime,
        end: datetime,
        timezone: str,
        attendees: list[str],
        idempotency_key: str,
    ) -> str:
        # Google event IDs use base32hex characters only. The campaign key is already a hash.
        event_id = idempotency_key[:32].lower().replace("-", "")
        body = {
            "id": event_id,
            "summary": summary,
            "description": description,
            "start": {"dateTime": start.isoformat(), "timeZone": timezone},
            "end": {"dateTime": end.isoformat(), "timeZone": timezone},
            "attendees": [{"email": item} for item in attendees],
            "extendedProperties": {"private": {"jneoCampaignId": idempotency_key}},
        }
        try:
            item = self._service().events().insert(calendarId="primary", body=body).execute()
        except Exception as exc:
            response = getattr(exc, "resp", None)
            status = getattr(exc, "status_code", None) or getattr(response, "status", None)
            if status != 409:
                raise
            item = self._service().events().get(calendarId="primary", eventId=event_id).execute()
        return item["id"]
