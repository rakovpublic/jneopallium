# Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
"""Shared frame schema + JSONL/UDS I/O helpers for the Nengo <-> Jneopallium
bridge (15-NENGO.md §9.1).

The schema is the single source of truth on the Python side; the Java
side mirrors the same fields in :class:`NengoInputFrame` /
:class:`NengoOutputFrame` records. Anything that doesn't fit the schema
is rejected on both sides (per 00-FRAMEWORK §3).
"""

from __future__ import annotations

import dataclasses
import json
import math
import os
import socket
import time
from typing import Dict, Iterator, Optional

SCHEMA_VERSION = "1"
SOURCE_NENGO_INPUT = "NENGO_INPUT"
SOURCE_JNEO_OUTPUT = "JNEOPALLIUM_OUTPUT"

STATUS_OK = "OK"
STATUS_DEGRADED = "DEGRADED"
STATUS_STOP = "STOP"


def now_ms() -> int:
    return int(time.time() * 1000)


@dataclasses.dataclass
class NengoFrame:
    schema_version: str
    source: str
    frame_id: str
    sequence_no: int
    timestamp_ms: int
    valid_until_ms: int
    safety_status: str
    values: Dict[str, float]
    transparency_log_id: Optional[str] = None

    def to_jsonl(self) -> bytes:
        obj = dataclasses.asdict(self)
        # Drop None transparency_log_id so the Java side's
        # NON_NULL include matches byte-for-byte.
        if obj.get("transparency_log_id") is None:
            obj.pop("transparency_log_id", None)
        return (json.dumps(obj, separators=(",", ":")) + "\n").encode("utf-8")

    @classmethod
    def from_line(cls, line: str) -> "NengoFrame":
        obj = json.loads(line)
        return cls(
            schema_version=obj["schema_version"],
            source=obj["source"],
            frame_id=obj["frame_id"],
            sequence_no=int(obj["sequence_no"]),
            timestamp_ms=int(obj["timestamp_ms"]),
            valid_until_ms=int(obj["valid_until_ms"]),
            safety_status=obj["safety_status"],
            values={k: float(v) for k, v in obj.get("values", {}).items()},
            transparency_log_id=obj.get("transparency_log_id"),
        )

    def validate(self) -> Optional[str]:
        if self.schema_version != SCHEMA_VERSION:
            return f"SCHEMA_VERSION_MISMATCH:{self.schema_version}"
        if self.source not in (SOURCE_NENGO_INPUT, SOURCE_JNEO_OUTPUT):
            return f"BAD_SOURCE:{self.source}"
        if not self.frame_id:
            return "MISSING_FIELD:frame_id"
        if self.sequence_no < 0:
            return "BAD_FIELD:sequence_no"
        if self.safety_status not in (STATUS_OK, STATUS_DEGRADED, STATUS_STOP):
            return f"BAD_SAFETY_STATUS:{self.safety_status}"
        for k, v in self.values.items():
            if not math.isfinite(v):
                return f"NON_FINITE_VALUE:{k}"
        return None


# -----------------------------------------------------------------------------
# Channel helpers — UDS or append-only JSONL file.
# -----------------------------------------------------------------------------


class FileWriter:
    """Append-only JSONL writer. Deterministic — best for CI replay."""

    def __init__(self, path: str) -> None:
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        self._f = open(path, "ab", buffering=0)

    def write(self, data: bytes) -> None:
        self._f.write(data)

    def close(self) -> None:
        try:
            self._f.close()
        except Exception:
            pass

    def __enter__(self) -> "FileWriter":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


class FileReader:
    """Tail-style reader of a growing JSONL file."""

    def __init__(self, path: str) -> None:
        self._path = path
        self._f = None
        self._open_if_present()

    def _open_if_present(self) -> None:
        if self._f is None and os.path.exists(self._path):
            self._f = open(self._path, "r", encoding="utf-8")

    def poll_lines(self, max_lines: int = 128) -> Iterator[str]:
        self._open_if_present()
        if self._f is None:
            return iter(())
        out = []
        for _ in range(max_lines):
            line = self._f.readline()
            if not line:
                break
            line = line.rstrip("\n")
            if line:
                out.append(line)
        return iter(out)

    def close(self) -> None:
        try:
            if self._f is not None:
                self._f.close()
        except Exception:
            pass

    def __enter__(self) -> "FileReader":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


class UdsWriter:
    """Connects to a Unix-domain server socket and writes one JSONL line per call."""

    def __init__(self, path: str, retry_backoff_s: float = 0.25,
                 max_backoff_s: float = 5.0) -> None:
        self._path = path
        self._retry = retry_backoff_s
        self._max = max_backoff_s
        self._sock: Optional[socket.socket] = None

    def _connect(self) -> bool:
        if self._sock is not None:
            return True
        try:
            s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            s.connect(self._path)
            self._sock = s
            return True
        except (FileNotFoundError, ConnectionRefusedError, OSError):
            return False

    def write(self, data: bytes) -> bool:
        backoff = self._retry
        while True:
            if not self._connect():
                time.sleep(min(backoff, self._max))
                backoff = min(backoff * 2, self._max)
                if backoff >= self._max:
                    return False
                continue
            try:
                self._sock.sendall(data)
                return True
            except (BrokenPipeError, ConnectionResetError, OSError):
                self.close()
                continue

    def close(self) -> None:
        if self._sock is not None:
            try:
                self._sock.close()
            except Exception:
                pass
            self._sock = None

    def __enter__(self) -> "UdsWriter":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


class UdsReader:
    """Connects to a Unix-domain server socket and reads JSONL lines."""

    def __init__(self, path: str, retry_backoff_s: float = 0.25,
                 max_backoff_s: float = 5.0) -> None:
        self._path = path
        self._retry = retry_backoff_s
        self._max = max_backoff_s
        self._sock: Optional[socket.socket] = None
        self._buf = b""

    def _connect(self) -> bool:
        if self._sock is not None:
            return True
        try:
            s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            s.connect(self._path)
            s.setblocking(False)
            self._sock = s
            return True
        except (FileNotFoundError, ConnectionRefusedError, OSError):
            return False

    def poll_lines(self, max_lines: int = 128) -> Iterator[str]:
        if not self._connect():
            return iter(())
        out = []
        try:
            while len(out) < max_lines:
                chunk = self._sock.recv(8192)
                if not chunk:
                    break
                self._buf += chunk
                while b"\n" in self._buf and len(out) < max_lines:
                    line, self._buf = self._buf.split(b"\n", 1)
                    s = line.decode("utf-8", errors="replace").rstrip()
                    if s:
                        out.append(s)
        except BlockingIOError:
            # No more data available right now.
            pass
        except (ConnectionResetError, OSError):
            self.close()
        return iter(out)

    def close(self) -> None:
        if self._sock is not None:
            try:
                self._sock.close()
            except Exception:
                pass
            self._sock = None

    def __enter__(self) -> "UdsReader":
        return self

    def __exit__(self, *exc) -> None:
        self.close()


def open_channel_a(path: str, mode: str = "UDS"):
    """Channel A — Nengo writes input frames.

    ``mode`` is either ``"UDS"`` (default) or ``"FILE"``.
    """
    if mode == "FILE":
        return FileWriter(path)
    return UdsWriter(path)


def open_channel_b(path: str, mode: str = "UDS"):
    """Channel B — Nengo reads approved output frames."""
    if mode == "FILE":
        return FileReader(path)
    return UdsReader(path)
