from __future__ import annotations

import ipaddress
import re
from dataclasses import dataclass
from html import escape
from pathlib import Path
from urllib.parse import urlparse

import bleach

SECRET_KEY_PATTERN = re.compile(
    r"(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|password)"
)
TOKEN_PATTERN = re.compile(r"(?i)(bearer\s+)[A-Za-z0-9._~+/=-]+")
INJECTION_PATTERNS = [
    re.compile(pattern, re.I)
    for pattern in (
        r"ignore (all|any|the|your) (previous|prior|system) instructions",
        r"system prompt",
        r"developer message",
        r"reveal .*credential",
        r"change .*sending limit",
        r"execute .*command",
        r"tool call",
    )
]


def redact_secrets(value: object) -> object:
    if isinstance(value, dict):
        return {
            str(key): "[REDACTED]" if SECRET_KEY_PATTERN.search(str(key)) else redact_secrets(item)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [redact_secrets(item) for item in value]
    if isinstance(value, str):
        return TOKEN_PATTERN.sub(r"\1[REDACTED]", value)
    return value


def sanitize_html(value: str) -> str:
    return bleach.clean(
        value,
        tags=["p", "br", "strong", "em", "ul", "ol", "li", "a", "code"],
        attributes={"a": ["href", "title"]},
        protocols=["https", "mailto"],
        strip=True,
    )


@dataclass(frozen=True)
class ExternalContent:
    text: str
    prompt_injection_suspected: bool
    matched_patterns: tuple[str, ...]

    def as_data_block(self) -> str:
        # Delimiters are data provenance markers, never model instructions.
        return f"<UNTRUSTED_EXTERNAL_DATA>\n{self.text}\n</UNTRUSTED_EXTERNAL_DATA>"


def sanitize_external_content(value: str, max_chars: int = 20_000) -> ExternalContent:
    text = "".join(ch for ch in value if ch in "\n\t" or ord(ch) >= 32)[:max_chars]
    matched = tuple(pattern.pattern for pattern in INJECTION_PATTERNS if pattern.search(text))
    return ExternalContent(
        text=text, prompt_injection_suspected=bool(matched), matched_patterns=matched
    )


def validate_outbound_url(value: str) -> str:
    parsed = urlparse(value)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("Outbound URLs must be credential-free HTTPS URLs")
    host = parsed.hostname.lower()
    if host in {"localhost", "localhost.localdomain"} or host.endswith(".local"):
        raise ValueError("Local outbound URLs are forbidden")
    try:
        address = ipaddress.ip_address(host)
    except ValueError:
        return value
    if not address.is_global:
        raise ValueError("Private or non-global outbound addresses are forbidden")
    return value


def neutralize_spreadsheet_formula(value: str) -> str:
    return "'" + value if value.lstrip().startswith(("=", "+", "-", "@")) else value


def validate_attachment(path: Path, max_bytes: int = 5_000_000) -> None:
    allowed = {".pdf", ".txt", ".md", ".png", ".jpg", ".jpeg"}
    if path.suffix.lower() not in allowed:
        raise ValueError(f"Attachment type {path.suffix} is not allowed")
    if path.stat().st_size > max_bytes:
        raise ValueError("Attachment exceeds the configured size limit")


def text_to_safe_html(value: str) -> str:
    paragraphs = [f"<p>{escape(part)}</p>" for part in value.split("\n\n") if part.strip()]
    return sanitize_html("\n".join(paragraphs))
