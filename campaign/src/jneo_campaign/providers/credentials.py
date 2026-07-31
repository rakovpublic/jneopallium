from __future__ import annotations

import base64
import json
import os
from pathlib import Path

import keyring
from cryptography.fernet import Fernet, InvalidToken


class CredentialStoreError(RuntimeError):
    pass


class EncryptedCredentialStore:
    """Encrypt tokens at rest; keep the envelope key in the OS credential store."""

    SERVICE = "jneo-campaign-oauth"

    def __init__(self, path: Path, account: str) -> None:
        self.path = path
        self.account = account or "default"

    def _key(self, create: bool = False) -> bytes:
        try:
            stored = keyring.get_password(self.SERVICE, self.account)
        except Exception as exc:
            stored = None
            keyring_error = exc
        else:
            keyring_error = None
        if stored:
            return stored.encode()
        environment_key = os.getenv("CAMPAIGN_TOKEN_ENCRYPTION_KEY")
        if environment_key:
            try:
                base64.urlsafe_b64decode(environment_key)
            except Exception as exc:
                raise CredentialStoreError("Invalid CAMPAIGN_TOKEN_ENCRYPTION_KEY") from exc
            return environment_key.encode()
        if not create:
            detail = f": {keyring_error}" if keyring_error else ""
            raise CredentialStoreError(f"No credential encryption key is available{detail}")
        key = Fernet.generate_key()
        try:
            keyring.set_password(self.SERVICE, self.account, key.decode())
        except Exception as exc:
            raise CredentialStoreError(
                "The OS credential store is unavailable; set CAMPAIGN_TOKEN_ENCRYPTION_KEY"
            ) from exc
        return key

    def save(self, payload: dict[str, object]) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        encrypted = Fernet(self._key(create=True)).encrypt(
            json.dumps(payload, separators=(",", ":")).encode()
        )
        self.path.write_bytes(encrypted)

    def load(self) -> dict[str, object]:
        if not self.path.exists():
            raise CredentialStoreError(f"Credential token file does not exist: {self.path}")
        try:
            clear = Fernet(self._key()).decrypt(self.path.read_bytes())
        except InvalidToken as exc:
            raise CredentialStoreError("Credential token could not be decrypted") from exc
        payload = json.loads(clear)
        if not isinstance(payload, dict):
            raise CredentialStoreError("Credential token has an invalid structure")
        return payload
