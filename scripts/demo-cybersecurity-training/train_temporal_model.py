#!/usr/bin/env python3
"""Train the Demo 06 temporal cybersecurity correlation model.

The trainer has two modes:

* Reference mode, used by this repository, builds a deterministic
  multi-source corpus that exercises the LANL, ToN_IoT, OpTC,
  CIC/CSE-CIC, UNSW-NB15, and CALDERA mappings without bundling those
  large public datasets.
* Manifest mode accepts external canonical JSONL/CSV sources and keeps
  the same leakage-safe campaign / host / time split policy.

No third-party Python packages are required.
"""

from __future__ import annotations

import argparse
import csv
import gzip
import hashlib
import json
import math
import re
import statistics
from glob import glob
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


SOURCE_IDS = ["LANL", "ToN_IoT", "OpTC", "CIC-IDS2017", "CSE-CIC-IDS2018", "UNSW-NB15", "CALDERA"]
TECHNIQUES = [
    "UNUSUAL_LOGIN",
    "PRIVILEGE_ESCALATION",
    "CREDENTIAL_ACCESS",
    "EXECUTION",
    "DNS_LOOKUP",
    "LATERAL_MOVEMENT",
    "COMMAND_AND_CONTROL",
    "EXFILTRATION",
    "NETWORK_ATTACK",
    "BENIGN_CONTEXT",
]
FEATURE_NAMES = [
    "event_count",
    "duration_ticks",
    "source_diversity",
    "max_evidence",
    "mean_evidence",
    "sum_evidence",
    "max_threat_intel",
    "mean_asset_criticality",
    "maintenance_ratio",
    "benign_context_ratio",
    "ordered_login_to_execution",
    "ordered_execution_to_lateral",
    "ordered_lateral_to_c2",
    "ordered_c2_to_exfiltration",
    "ordered_credential_to_lateral",
    "network_receptor_score",
    "host_receptor_score",
    "slow_context_score",
] + [f"technique_{name.lower()}" for name in TECHNIQUES] + [
    "source_lanl",
    "source_toniot",
    "source_optc",
    "source_cic",
    "source_unsw",
    "source_caldera",
]


@dataclass(frozen=True)
class Event:
    dataset: str
    source_kind: str
    entity_id: str
    event_tick: int
    source: str
    event_type: str
    technique: str
    evidence_confidence: float
    threat_intel_confidence: float
    asset_criticality: float
    maintenance_active: bool
    malicious: bool
    campaign_id: str
    host_group: str
    attack_type: str
    split: str = ""


@dataclass(frozen=True)
class Window:
    window_id: str
    split: str
    label: int
    campaign_id: str
    host_group: str
    attack_type: str
    events: tuple[Event, ...]
    features: tuple[float, ...]


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, float(value)))


def stable_bucket(value: str, modulo: int = 100) -> int:
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
    return int(digest[:8], 16) % modulo


def stable_unit(value: str) -> float:
    return stable_bucket(value, 10_000) / 9_999.0


def deterministic_jitter(value: str, amplitude: float) -> float:
    return (stable_unit(value) - 0.5) * 2.0 * amplitude


def sigmoid(value: float) -> float:
    if value >= 0:
        z = math.exp(-value)
        return 1.0 / (1.0 + z)
    z = math.exp(value)
    return z / (1.0 + z)


def read_rows(path: Path) -> Iterable[dict[str, str]]:
    opener = gzip.open if path.suffix == ".gz" else open
    if path.suffixes[-2:] == [".jsonl", ".gz"] or path.suffix == ".jsonl":
        with opener(path, "rt", encoding="utf-8", newline="") as handle:
            for line in handle:
                if line.strip():
                    yield json.loads(line)
        return
    with opener(path, "rt", encoding="utf-8", newline="") as handle:
        sample = handle.read(4096)
        handle.seek(0)
        dialect = csv.Sniffer().sniff(sample) if sample else csv.excel
        reader = csv.DictReader(handle, dialect=dialect)
        for row in reader:
            yield {str(k): "" if v is None else str(v) for k, v in row.items()}


LANL_COLUMNS = {
    "lanl-auth": [
        "time", "source_user", "destination_user", "source_computer", "destination_computer",
        "authentication_type", "logon_type", "authentication_orientation", "result",
    ],
    "lanl-proc": ["time", "user", "computer", "process_name", "action"],
    "lanl-flow": [
        "time", "duration", "source_computer", "source_port", "destination_computer",
        "destination_port", "protocol", "packet_count", "byte_count",
    ],
    "lanl-dns": ["time", "source_computer", "resolved_computer"],
    "lanl-redteam": ["time", "user", "source_computer", "destination_computer"],
}


def read_source_rows(path: Path, source: dict) -> Iterable[dict[str, str]]:
    kind = str(source.get("kind", "canonical")).lower()
    if kind in LANL_COLUMNS:
        opener = gzip.open if path.suffix == ".gz" else open
        with opener(path, "rt", encoding="utf-8", newline="") as handle:
            reader = csv.reader(handle)
            columns = LANL_COLUMNS[kind]
            for values in reader:
                if not values:
                    continue
                yield {name: values[i] if i < len(values) else "" for i, name in enumerate(columns)}
        return
    yield from read_rows(path)


def manifest_paths(manifest_path: Path, raw_path: str) -> list[Path]:
    path = Path(raw_path)
    resolved = path if path.is_absolute() else manifest_path.parent / path
    text = str(resolved)
    if any(marker in text for marker in ("*", "?", "[")):
        return [Path(match) for match in sorted(glob(text, recursive=True))]
    return [resolved]


def get_any(row: dict[str, str], *names: str, default: str = "") -> str:
    lowered = {str(key).lower(): value for key, value in row.items()}
    for name in names:
        if name in row and str(row[name]).strip() != "":
            return str(row[name]).strip()
        value = lowered.get(name.lower())
        if value is not None and str(value).strip() != "":
            return str(value).strip()
    return default


def parse_tick(value: str, default: int = 0) -> int:
    text = str(value or "").strip()
    if not text:
        return default
    try:
        return int(float(text))
    except ValueError:
        pass
    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S", "%d/%m/%Y %H:%M:%S",
                "%m/%d/%Y %H:%M:%S", "%d-%b-%Y %H:%M:%S"):
        try:
            parsed = datetime.strptime(text.replace("Z", ""), fmt)
            return int(parsed.replace(tzinfo=timezone.utc).timestamp())
        except ValueError:
            continue
    return default


def lanl_campaign(host: str, tick: int) -> str:
    safe_host = host or "unknown-host"
    return f"LANL:{safe_host}:{tick // 7200}"


def lanl_event(row: dict[str, str], source: dict) -> Event:
    kind = str(source.get("kind", "")).lower()
    tick = parse_tick(get_any(row, "time"))
    split = str(source.get("split", "")).strip()
    if kind == "lanl-auth":
        destination = get_any(row, "destination_computer", default="unknown-host")
        result = get_any(row, "result")
        failed = result.lower() not in {"success", "succeeded"}
        return Event(
            dataset="LANL",
            source_kind=kind,
            entity_id=get_any(row, "destination_user", "source_user", default=destination),
            event_tick=tick,
            source="auth",
            event_type="authentication",
            technique="UNUSUAL_LOGIN",
            evidence_confidence=0.42 if failed else 0.24,
            threat_intel_confidence=0.0,
            asset_criticality=0.55,
            maintenance_active=False,
            malicious=False,
            campaign_id=lanl_campaign(destination, tick),
            host_group=destination,
            attack_type="lanl_authentication",
            split=split,
        )
    if kind == "lanl-proc":
        host = get_any(row, "computer", default="unknown-host")
        action = get_any(row, "action")
        return Event(
            dataset="LANL",
            source_kind=kind,
            entity_id=host,
            event_tick=tick,
            source="process",
            event_type=f"process_{action.lower() or 'event'}",
            technique="EXECUTION",
            evidence_confidence=0.32 if action.lower() == "start" else 0.18,
            threat_intel_confidence=0.0,
            asset_criticality=0.55,
            maintenance_active=False,
            malicious=False,
            campaign_id=lanl_campaign(host, tick),
            host_group=host,
            attack_type="lanl_process",
            split=split,
        )
    if kind == "lanl-flow":
        source_host = get_any(row, "source_computer", default="unknown-host")
        destination_host = get_any(row, "destination_computer", default="unknown-host")
        byte_count = parse_tick(get_any(row, "byte_count"), 0)
        packet_count = parse_tick(get_any(row, "packet_count"), 0)
        evidence = clamp01(0.22 + min(math.log10(max(1, byte_count + packet_count)) / 12.0, 0.34))
        return Event(
            dataset="LANL",
            source_kind=kind,
            entity_id=f"{source_host}->{destination_host}",
            event_tick=tick,
            source="flow",
            event_type="network_flow",
            technique="NETWORK_ATTACK",
            evidence_confidence=evidence,
            threat_intel_confidence=0.0,
            asset_criticality=0.55,
            maintenance_active=False,
            malicious=False,
            campaign_id=lanl_campaign(source_host, tick),
            host_group=source_host,
            attack_type="lanl_network_flow",
            split=split,
        )
    if kind == "lanl-dns":
        source_host = get_any(row, "source_computer", default="unknown-host")
        resolved = get_any(row, "resolved_computer", default="unknown-destination")
        return Event(
            dataset="LANL",
            source_kind=kind,
            entity_id=f"{source_host}->{resolved}",
            event_tick=tick,
            source="dns",
            event_type="dns_lookup",
            technique="DNS_LOOKUP",
            evidence_confidence=0.28,
            threat_intel_confidence=0.0,
            asset_criticality=0.55,
            maintenance_active=False,
            malicious=False,
            campaign_id=lanl_campaign(source_host, tick),
            host_group=source_host,
            attack_type="lanl_dns",
            split=split,
        )
    if kind == "lanl-redteam":
        destination = get_any(row, "destination_computer", default="unknown-host")
        return Event(
            dataset="LANL",
            source_kind=kind,
            entity_id=get_any(row, "user", default=destination),
            event_tick=tick,
            source="auth",
            event_type="redteam_compromise",
            technique="LATERAL_MOVEMENT",
            evidence_confidence=0.95,
            threat_intel_confidence=0.90,
            asset_criticality=0.85,
            maintenance_active=False,
            malicious=True,
            campaign_id=lanl_campaign(destination, tick),
            host_group=destination,
            attack_type="lanl_redteam_compromise",
            split=split,
        )
    raise ValueError(f"Unsupported LANL source kind: {kind}")


def technique_from_attack(value: str, source_kind: str) -> str:
    text = str(value or "").strip().lower().replace("-", "_").replace(" ", "_")
    if text in {"", "normal", "benign", "none", "0"}:
        return "BENIGN_CONTEXT"
    if any(token in text for token in ("password", "brute", "credential")):
        return "CREDENTIAL_ACCESS"
    if any(token in text for token in ("ddos", "dos", "scan", "xss", "injection", "mitm", "backdoor")):
        return "NETWORK_ATTACK"
    if "ransom" in text or "process" in source_kind or "windows" in source_kind or "linux" in source_kind:
        return "EXECUTION"
    if "dns" in text:
        return "DNS_LOOKUP"
    if "c2" in text or "command" in text:
        return "COMMAND_AND_CONTROL"
    if "exfil" in text:
        return "EXFILTRATION"
    return "NETWORK_ATTACK"


def toniot_event(row: dict[str, str], source: dict) -> Event:
    kind = str(source.get("kind", "toniot")).lower()
    tick = parse_tick(get_any(row, "event_tick", "ts", "time", "timestamp", "date", "datetime"))
    label = get_any(row, "malicious", "label", "Label", "class", "Class")
    attack_type = get_any(row, "attack_type", "type", "Type", "attack_cat", "category", "Category", default="normal")
    malicious = parse_bool(label) or attack_type.strip().lower() not in {"", "normal", "benign", "none", "0"}
    src = get_any(row, "src_ip", "srcip", "source_ip", "source", "Source", "src", default="")
    dst = get_any(row, "dst_ip", "dstip", "destination_ip", "destination", "Destination", "dst", default="")
    host = get_any(row, "host", "hostname", "computer", "machine", "ip", "IP", default=src or dst or "toniot-entity")
    entity = get_any(row, "entity_id", default=f"{src}->{dst}" if src or dst else host)
    signal_source = str(source.get("signalSource", "")).strip()
    if not signal_source:
        signal_source = "flow" if "network" in kind or "zeek" in kind else "process"
    technique = normalise_technique(get_any(row, "technique", default=technique_from_attack(attack_type, kind)))
    split = get_any(row, "split", default=str(source.get("split", "")).strip())
    evidence = clamp01(float(source.get("maliciousEvidence", 0.74 if malicious else 0.22)))
    intel = clamp01(float(source.get("maliciousThreatIntel", 0.35 if malicious else 0.0)))
    return Event(
        dataset="ToN_IoT",
        source_kind=kind,
        entity_id=entity,
        event_tick=tick,
        source=signal_source,
        event_type=get_any(row, "event_type", "service", "proto", "protocol", default=attack_type or signal_source),
        technique=technique,
        evidence_confidence=evidence,
        threat_intel_confidence=intel,
        asset_criticality=clamp01(float(source.get("assetCriticality", 0.60))),
        maintenance_active=parse_bool(get_any(row, "maintenance_active", default="false")),
        malicious=malicious,
        campaign_id=get_any(row, "campaign_id", default=f"ToN_IoT:{host}:{attack_type}:{tick // 7200}"),
        host_group=get_any(row, "host_group", default=host),
        attack_type=attack_type,
        split=split,
    )


def row_event(row: dict[str, str], source: dict) -> Event:
    kind = str(source.get("kind", "canonical")).lower()
    if kind.startswith("lanl-"):
        return lanl_event(row, source)
    if kind.startswith("toniot-"):
        return toniot_event(row, source)

    def get(name: str, default: str = "") -> str:
        aliases = source.get("columns", {}).get(name, [name])
        if isinstance(aliases, str):
            aliases = [aliases]
        for alias in aliases:
            if alias in row and str(row[alias]).strip() != "":
                return str(row[alias]).strip()
        return default

    dataset = source.get("id", "external")
    kind = source.get("kind", "canonical")
    entity = get("entity_id") or get("host") or get("user") or "unknown-entity"
    tick = int(float(get("event_tick", get("time", "0"))))
    technique = normalise_technique(get("technique", get("attack_technique", "UNKNOWN")))
    event_type = get("event_type", technique.lower())
    malicious = parse_bool(get("malicious", get("label", "false")))
    split = get("split", "")
    return Event(
        dataset=dataset,
        source_kind=kind,
        entity_id=entity,
        event_tick=tick,
        source=get("source", kind),
        event_type=event_type,
        technique=technique,
        evidence_confidence=clamp01(float(get("evidence_confidence", "0.25"))),
        threat_intel_confidence=clamp01(float(get("threat_intel_confidence", "0.0"))),
        asset_criticality=clamp01(float(get("asset_criticality", "0.5"))),
        maintenance_active=parse_bool(get("maintenance_active", "false")),
        malicious=malicious,
        campaign_id=get("campaign_id", f"{dataset}:{entity}:{tick // 7200}"),
        host_group=get("host_group", entity.split("@")[-1]),
        attack_type=get("attack_type", technique),
        split=split,
    )


def parse_bool(value: str) -> bool:
    return str(value).strip().lower() in {"1", "true", "yes", "y", "malicious", "attack"}


def normalise_technique(value: str) -> str:
    text = str(value or "UNKNOWN").strip().upper().replace("-", "_").replace(" ", "_")
    aliases = {
        "AUTH": "UNUSUAL_LOGIN",
        "AUTHENTICATION": "UNUSUAL_LOGIN",
        "PROCESS": "EXECUTION",
        "POWERSHELL": "EXECUTION",
        "DNS": "DNS_LOOKUP",
        "C2": "COMMAND_AND_CONTROL",
        "COMMAND_AND_CONTROL_TRAFFIC": "COMMAND_AND_CONTROL",
        "FLOW": "NETWORK_ATTACK",
        "MAINTENANCE": "BENIGN_CONTEXT",
        "BENIGN": "BENIGN_CONTEXT",
    }
    return aliases.get(text, text)


def external_events(manifest_path: Path) -> list[Event]:
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    events: list[Event] = []
    for source in manifest.get("sources", []):
        paths = source.get("paths") or source.get("files") or []
        if isinstance(paths, dict):
            paths = list(paths.values())
        if isinstance(paths, str):
            paths = [paths]
        max_rows = int(source.get("maxRows", manifest.get("maxRowsPerSource", 0)) or 0)
        selected = 0
        for raw_path in paths:
            matched_paths = manifest_paths(manifest_path, raw_path)
            if not matched_paths and manifest.get("requireAllSources", False):
                raise FileNotFoundError(raw_path)
            for path in matched_paths:
                path = path.resolve()
                if not path.exists():
                    if manifest.get("requireAllSources", False):
                        raise FileNotFoundError(path)
                    continue
                for row in read_source_rows(path, source):
                    event = row_event(row, source)
                    if not include_event(event, source):
                        continue
                    events.append(event)
                    selected += 1
                    if max_rows and selected >= max_rows:
                        break
                if max_rows and selected >= max_rows:
                    break
            if max_rows and selected >= max_rows:
                break
    return events


def include_event(event: Event, source: dict) -> bool:
    start_tick = source.get("startTick")
    end_tick = source.get("endTick")
    if start_tick is not None and event.event_tick < int(start_tick):
        return False
    if end_tick is not None and event.event_tick > int(end_tick):
        return False
    modulo = int(source.get("sampleModulo", 0) or 0)
    if modulo > 1:
        keep = int(source.get("sampleKeep", 0) or 0)
        key = f"{event.campaign_id}:{event.entity_id}:{event.event_tick}:{event.technique}"
        if stable_bucket(key, modulo) != keep:
            return False
    return True


def reference_events() -> list[Event]:
    events: list[Event] = []

    def add(dataset: str, campaign: str, split: str, label: bool, host: str, attack_type: str,
            start: int, sequence: list[tuple[int, str, str, float, float, float, bool]]) -> None:
        for offset, source, technique, evidence, intel, criticality, maintenance in sequence:
            events.append(Event(
                dataset=dataset,
                source_kind=source,
                entity_id=host,
                event_tick=start + offset,
                source=source,
                event_type=technique.lower(),
                technique=technique,
                evidence_confidence=evidence,
                threat_intel_confidence=intel,
                asset_criticality=criticality,
                maintenance_active=maintenance,
                malicious=label,
                campaign_id=campaign,
                host_group=host.split("@")[-1],
                attack_type=attack_type,
                split=split,
            ))

    attack_chain = [
        (0, "auth", "UNUSUAL_LOGIN", 0.72, 0.00, 0.80, False),
        (2, "process", "EXECUTION", 0.86, 0.00, 0.80, False),
        (4, "auth", "CREDENTIAL_ACCESS", 0.78, 0.00, 0.80, False),
        (6, "auth", "LATERAL_MOVEMENT", 0.88, 0.00, 0.90, False),
        (8, "dns", "DNS_LOOKUP", 0.65, 0.70, 0.90, False),
        (10, "flow", "COMMAND_AND_CONTROL", 0.76, 0.75, 0.90, False),
    ]
    slow_exfil = [
        (0, "flow", "NETWORK_ATTACK", 0.38, 0.42, 0.75, False),
        (180, "flow", "NETWORK_ATTACK", 0.42, 0.48, 0.75, False),
        (360, "dns", "COMMAND_AND_CONTROL", 0.52, 0.66, 0.75, False),
        (720, "flow", "EXFILTRATION", 0.58, 0.66, 0.85, False),
    ]
    benign_maintenance = [
        (0, "maintenance", "BENIGN_CONTEXT", 0.08, 0.00, 0.45, True),
        (2, "auth", "BENIGN_CONTEXT", 0.20, 0.00, 0.45, True),
        (4, "process", "BENIGN_CONTEXT", 0.22, 0.00, 0.45, True),
        (8, "flow", "BENIGN_CONTEXT", 0.16, 0.00, 0.45, True),
    ]
    benign_admin = [
        (0, "auth", "UNUSUAL_LOGIN", 0.28, 0.00, 0.40, True),
        (3, "process", "EXECUTION", 0.25, 0.00, 0.40, True),
        (8, "maintenance", "BENIGN_CONTEXT", 0.30, 0.00, 0.40, True),
    ]
    network_attack = [
        (0, "flow", "NETWORK_ATTACK", 0.82, 0.15, 0.55, False),
        (1, "flow", "NETWORK_ATTACK", 0.86, 0.15, 0.55, False),
        (3, "dns", "COMMAND_AND_CONTROL", 0.52, 0.50, 0.60, False),
    ]

    add("LANL", "lanl-cred-train-a", "train", True, "user:svc-backup@wrk-17", "lateral_movement", 100, attack_chain)
    add("ToN_IoT", "toniot-c2-train-b", "train", True, "host:win10-03", "command_and_control", 500, attack_chain)
    add("OpTC", "optc-apt-train-h", "train", True, "host:win-enterprise-apt-07", "apt_chain", 700,
        attack_chain + slow_exfil)
    add("LANL", "lanl-slow-train-i", "train", True, "host:file-share-04", "exfiltration", 760, slow_exfil)
    add("CALDERA", "caldera-exfil-train-j", "train", True, "host:caldera-victim-03", "exfiltration", 820,
        slow_exfil)
    add("CIC-IDS2017", "cic-flow-train-c", "train", True, "flow:10.1.1.5->185.12.1.9", "network_attack", 900, network_attack)
    add("UNSW-NB15", "unsw-flow-train-d", "train", True, "flow:dmz-4->external", "network_attack", 1200, network_attack)
    add("LANL", "lanl-maint-train-e", "train", False, "svc:deploy@web-tier", "benign_maintenance", 2000, benign_maintenance)
    add("ToN_IoT", "toniot-admin-train-f", "train", False, "user:admin@iot-gateway", "benign_admin", 2300, benign_admin)
    add("OpTC", "optc-benign-train-g", "train", False, "host:win-enterprise-22", "benign_admin", 2600, benign_admin)
    add("CALDERA", "caldera-password-val-a", "validation", True, "user:helpdesk@dc-lab", "credential_access", 3000, attack_chain)
    add("OpTC", "optc-apt-val-b", "validation", True, "host:workstation-41", "apt_chain", 3400, attack_chain + slow_exfil)
    add("OpTC", "optc-slow-val-f", "validation", True, "host:finance-staging-02", "exfiltration", 3600,
        slow_exfil)
    add("CSE-CIC-IDS2018", "cse-cic-flow-val-c", "validation", True, "flow:corp->botnet", "network_attack", 3800, network_attack)
    add("CALDERA", "caldera-maint-val-d", "validation", False, "svc:patch@web-tier", "benign_maintenance", 4200, benign_maintenance)
    add("LANL", "lanl-admin-val-e", "validation", False, "user:domain-admin@mgmt", "benign_admin", 4500, benign_admin)
    add("CALDERA", "caldera-lateral-test-a", "test", True, "user:backup-service@workstation-17", "lateral_movement", 5000, attack_chain)
    add("OpTC", "optc-slow-test-b", "test", True, "host:finance-file-01", "exfiltration", 5400, slow_exfil)
    add("UNSW-NB15", "unsw-flow-test-c", "test", True, "flow:branch->internet", "network_attack", 6000, network_attack)
    add("LANL", "lanl-maint-test-d", "test", False, "svc:deployment-agent@web-tier", "benign_maintenance", 6400, benign_maintenance)
    add("ToN_IoT", "toniot-admin-test-e", "test", False, "user:ops@linux-server", "benign_admin", 6800, benign_admin)
    return events


def variant_entity_id(entity_id: str, replica: int) -> str:
    if replica == 0:
        return entity_id
    suffix = f"x{replica:04d}"
    if "@" in entity_id:
        principal, host = entity_id.rsplit("@", 1)
        return f"{principal}@{host}-{suffix}"
    return f"{entity_id}-{suffix}"


def variant_event(event: Event, replica: int) -> Event:
    if replica == 0:
        return event
    campaign_shift = stable_bucket(f"{event.campaign_id}:{replica}", 300)
    evidence = clamp01(event.evidence_confidence +
                       deterministic_jitter(f"evidence:{event.campaign_id}:{event.event_tick}:{replica}", 0.04))
    intel = clamp01(event.threat_intel_confidence +
                    deterministic_jitter(f"intel:{event.campaign_id}:{event.event_tick}:{replica}", 0.03))
    criticality = clamp01(event.asset_criticality +
                          deterministic_jitter(f"criticality:{event.host_group}:{replica}", 0.025))
    return Event(
        dataset=event.dataset,
        source_kind=event.source_kind,
        entity_id=variant_entity_id(event.entity_id, replica),
        event_tick=event.event_tick + replica * 10_000 + campaign_shift,
        source=event.source,
        event_type=event.event_type,
        technique=event.technique,
        evidence_confidence=evidence,
        threat_intel_confidence=intel,
        asset_criticality=criticality,
        maintenance_active=event.maintenance_active,
        malicious=event.malicious,
        campaign_id=f"{event.campaign_id}-x{replica:04d}",
        host_group=f"{event.host_group}-x{replica:04d}",
        attack_type=event.attack_type,
        split=event.split,
    )


def expand_reference_events(events: list[Event], multiplier: int) -> list[Event]:
    if multiplier < 1:
        raise ValueError("--reference-multiplier must be at least 1")
    if multiplier == 1:
        return events
    expanded: list[Event] = []
    for replica in range(multiplier):
        expanded.extend(variant_event(event, replica) for event in events)
    return expanded


def reference_replica_bytes(seed_events: list[Event], replica: int) -> int:
    return sum(len(canonical_event_line(variant_event(event, replica)).encode("utf-8"))
               for event in seed_events)


def estimate_reference_corpus_bytes(seed_events: list[Event], multiplier: int, samples: int = 512) -> int:
    if multiplier <= 0:
        return 0
    if multiplier <= samples:
        return sum(reference_replica_bytes(seed_events, replica) for replica in range(multiplier))
    sample_count = max(2, samples)
    sampled_replicas = {
        round(index * (multiplier - 1) / (sample_count - 1))
        for index in range(sample_count)
    }
    sampled_bytes = [reference_replica_bytes(seed_events, replica) for replica in sorted(sampled_replicas)]
    return int(statistics.fmean(sampled_bytes) * multiplier)


def reference_multiplier_for_target(seed_events: list[Event], target_bytes: int, max_bytes: int) -> int:
    if target_bytes <= 0:
        return 1
    limit = min(value for value in (target_bytes, max_bytes) if value > 0)
    bytes_per_replica = max(1, estimate_reference_corpus_bytes(seed_events, 256) // 256)
    high = max(1, int(limit / bytes_per_replica * 1.05) + 1)
    while estimate_reference_corpus_bytes(seed_events, high) < limit:
        high *= 2
    low = 1
    while low < high:
        mid = (low + high + 1) // 2
        if estimate_reference_corpus_bytes(seed_events, mid) <= limit:
            low = mid
        else:
            high = mid - 1
    return low


def window_split(events: tuple[Event, ...]) -> str:
    explicit = {event.split for event in events if event.split}
    if len(explicit) == 1:
        return next(iter(explicit))
    key = events[0].campaign_id or events[0].entity_id
    bucket = stable_bucket(key)
    if bucket < 70:
        return "train"
    if bucket < 85:
        return "validation"
    return "test"


def build_windows(events: list[Event]) -> list[Window]:
    grouped: dict[str, list[Event]] = {}
    for event in events:
        group = event.campaign_id or f"{event.entity_id}:{event.event_tick // 7200}"
        grouped.setdefault(group, []).append(event)
    windows: list[Window] = []
    for group, items in sorted(grouped.items()):
        ordered = tuple(sorted(items, key=lambda event: event.event_tick))
        label = 1 if any(event.malicious for event in ordered) else 0
        features = extract_features(ordered)
        windows.append(Window(
            window_id=group,
            split=window_split(ordered),
            label=label,
            campaign_id=ordered[0].campaign_id,
            host_group=ordered[0].host_group,
            attack_type=ordered[0].attack_type,
            events=ordered,
            features=features,
        ))
    return windows


def extract_features(events: tuple[Event, ...]) -> tuple[float, ...]:
    count = max(1, len(events))
    ticks = [event.event_tick for event in events]
    evidences = [event.evidence_confidence for event in events]
    sources = {event.source for event in events}
    techniques = [event.technique for event in events]
    technique_counts = {tech: techniques.count(tech) / count for tech in TECHNIQUES}
    source_ids = {event.dataset for event in events}

    values: dict[str, float] = {
        "event_count": min(count / 12.0, 1.0),
        "duration_ticks": min((max(ticks) - min(ticks)) / 7200.0, 1.0),
        "source_diversity": min(len(sources) / 6.0, 1.0),
        "max_evidence": max(evidences),
        "mean_evidence": statistics.fmean(evidences),
        "sum_evidence": min(sum(evidences) / 6.0, 1.0),
        "max_threat_intel": max(event.threat_intel_confidence for event in events),
        "mean_asset_criticality": statistics.fmean(event.asset_criticality for event in events),
        "maintenance_ratio": sum(1 for event in events if event.maintenance_active) / count,
        "benign_context_ratio": technique_counts.get("BENIGN_CONTEXT", 0.0),
        "ordered_login_to_execution": ordered_score(events, "UNUSUAL_LOGIN", "EXECUTION", 300, 180),
        "ordered_execution_to_lateral": ordered_score(events, "EXECUTION", "LATERAL_MOVEMENT", 600, 300),
        "ordered_lateral_to_c2": ordered_score(events, "LATERAL_MOVEMENT", "COMMAND_AND_CONTROL", 900, 420),
        "ordered_c2_to_exfiltration": ordered_score(events, "COMMAND_AND_CONTROL", "EXFILTRATION", 1800, 900),
        "ordered_credential_to_lateral": ordered_score(events, "CREDENTIAL_ACCESS", "LATERAL_MOVEMENT", 600, 240),
        "network_receptor_score": max((event.evidence_confidence for event in events
                                       if event.source in {"flow", "dns", "packet"}), default=0.0),
        "host_receptor_score": max((event.evidence_confidence for event in events
                                    if event.source in {"auth", "process"}), default=0.0),
        "slow_context_score": max((event.threat_intel_confidence + event.asset_criticality) / 2.0
                                  for event in events),
        "source_lanl": 1.0 if "LANL" in source_ids else 0.0,
        "source_toniot": 1.0 if "ToN_IoT" in source_ids else 0.0,
        "source_optc": 1.0 if "OpTC" in source_ids else 0.0,
        "source_cic": 1.0 if ("CIC-IDS2017" in source_ids or "CSE-CIC-IDS2018" in source_ids) else 0.0,
        "source_unsw": 1.0 if "UNSW-NB15" in source_ids else 0.0,
        "source_caldera": 1.0 if "CALDERA" in source_ids else 0.0,
    }
    for technique in TECHNIQUES:
        values[f"technique_{technique.lower()}"] = technique_counts.get(technique, 0.0)
    return tuple(values[name] for name in FEATURE_NAMES)


def ordered_score(events: tuple[Event, ...], first: str, second: str, window: int, tau: int) -> float:
    best = 0.0
    first_ticks = [event.event_tick for event in events if event.technique == first]
    second_ticks = [event.event_tick for event in events if event.technique == second]
    for a in first_ticks:
        for b in second_ticks:
            if b <= a or b - a > window:
                continue
            best = max(best, math.exp(-abs((b - a) - tau) / max(1.0, tau)))
    return best


def split_windows(windows: list[Window]) -> dict[str, list[Window]]:
    split = {"train": [], "validation": [], "test": []}
    for window in windows:
        split.setdefault(window.split, []).append(window)
    for name in ("validation", "test"):
        if not split[name]:
            split[name] = [window for window in windows if stable_bucket(window.window_id + name) >= 85][:1]
    if not split["train"]:
        split["train"] = [window for window in windows if window not in split["test"]]
    return split


def standardizer(windows: list[Window]) -> tuple[list[float], list[float]]:
    columns = list(zip(*(window.features for window in windows)))
    means = [statistics.fmean(column) for column in columns]
    stds = []
    for column, mean in zip(columns, means):
        variance = statistics.fmean((value - mean) ** 2 for value in column)
        stds.append(math.sqrt(variance) or 1.0)
    return means, stds


def scale(features: tuple[float, ...], means: list[float], stds: list[float]) -> list[float]:
    return [(value - mean) / std for value, mean, std in zip(features, means, stds)]


def train_logistic(windows: list[Window], epochs: int, learning_rate: float, l2: float,
                   max_windows_per_epoch: int = 0) -> dict:
    means, stds = standardizer(windows)
    scaled_features = [scale(window.features, means, stds) for window in windows]
    weights = [0.0] * len(FEATURE_NAMES)
    bias = 0.0
    positives = sum(window.label for window in windows)
    negatives = len(windows) - positives
    pos_weight = len(windows) / max(1, 2 * positives)
    neg_weight = len(windows) / max(1, 2 * negatives)
    order = sorted(range(len(windows)), key=lambda index: stable_bucket(windows[index].window_id, 1_000_000_007))
    epoch_size = len(windows)
    if max_windows_per_epoch > 0:
        epoch_size = min(max_windows_per_epoch, len(windows))
    for epoch in range(epochs):
        grad_w = [0.0] * len(weights)
        grad_b = 0.0
        lr = learning_rate / (1.0 + epoch / max(1, epochs))
        offset = (epoch * epoch_size) % max(1, len(order))
        indices = order if epoch_size == len(windows) else [
            order[(offset + i) % len(order)] for i in range(epoch_size)
        ]
        for index in indices:
            window = windows[index]
            x = scaled_features[index]
            y = float(window.label)
            weight = pos_weight if window.label else neg_weight
            prediction = sigmoid(bias + sum(w * v for w, v in zip(weights, x)))
            error = (prediction - y) * weight
            grad_b += error
            for i, value in enumerate(x):
                grad_w[i] += error * value + l2 * weights[i]
        n = max(1, len(indices))
        bias -= lr * grad_b / n
        for i in range(len(weights)):
            weights[i] -= lr * grad_w[i] / n
    return {"means": means, "stds": stds, "weights": weights, "bias": bias}


def predict(window: Window, model: dict) -> float:
    x = scale(window.features, model["means"], model["stds"])
    return sigmoid(model["bias"] + sum(w * v for w, v in zip(model["weights"], x)))


def choose_threshold(windows: list[Window], model: dict) -> float:
    candidates = [i / 100.0 for i in range(20, 91)]
    best_threshold = 0.5
    best_score = -1.0
    for threshold in candidates:
        metrics = evaluate(windows, model, threshold)
        score = metrics["f1"] - 0.35 * metrics["falsePositiveRate"] + 0.05 * metrics["recall"]
        if score > best_score:
            best_score = score
            best_threshold = threshold
    return best_threshold


def evaluate(windows: list[Window], model: dict, threshold: float) -> dict[str, float]:
    tp = fp = tn = fn = 0
    for window in windows:
        predicted = predict(window, model) >= threshold
        if predicted and window.label:
            tp += 1
        elif predicted:
            fp += 1
        elif window.label:
            fn += 1
        else:
            tn += 1
    precision = tp / max(1, tp + fp)
    recall = tp / max(1, tp + fn)
    f1 = 2 * precision * recall / max(1e-12, precision + recall)
    accuracy = (tp + tn) / max(1, tp + tn + fp + fn)
    fpr = fp / max(1, fp + tn)
    return {
        "count": len(windows),
        "truePositive": tp,
        "falsePositive": fp,
        "trueNegative": tn,
        "falseNegative": fn,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "accuracy": accuracy,
        "falsePositiveRate": fpr,
        "meanTimeToDetectionTicks": mean_time_to_detection(windows, model, threshold),
    }


def mean_time_to_detection(windows: list[Window], model: dict, threshold: float) -> float:
    delays = []
    for window in windows:
        if not window.label:
            continue
        start = window.events[0].event_tick
        detected_at = None
        for i in range(1, len(window.events) + 1):
            prefix_events = window.events[:i]
            prefix = Window(
                window_id=window.window_id,
                split=window.split,
                label=window.label,
                campaign_id=window.campaign_id,
                host_group=window.host_group,
                attack_type=window.attack_type,
                events=prefix_events,
                features=extract_features(prefix_events),
            )
            if predict(prefix, model) >= threshold:
                detected_at = prefix_events[-1].event_tick
                break
        if detected_at is not None:
            delays.append(max(0, detected_at - start))
    return statistics.fmean(delays) if delays else 0.0


def calibration(windows: list[Window], model: dict) -> list[dict]:
    bins: list[dict] = []
    for low in [i / 10 for i in range(10)]:
        high = low + 0.1
        bucket = [window for window in windows if low <= predict(window, model) < high]
        if not bucket:
            continue
        bins.append({
            "low": low,
            "high": high,
            "count": len(bucket),
            "positiveRate": sum(window.label for window in bucket) / len(bucket),
            "meanScore": statistics.fmean(predict(window, model) for window in bucket),
        })
    return bins


def source_counts(events: list[Event]) -> dict[str, int]:
    counts = {source: 0 for source in SOURCE_IDS}
    for event in events:
        counts[event.dataset] = counts.get(event.dataset, 0) + 1
    return counts


def scaled_counts(counts: dict[str, int], multiplier: int) -> dict[str, int]:
    return {name: count * multiplier for name, count in counts.items()}


SIZE_UNITS = {
    "": 1,
    "b": 1,
    "kb": 1024,
    "kib": 1024,
    "mb": 1024 ** 2,
    "mib": 1024 ** 2,
    "gb": 1024 ** 3,
    "gib": 1024 ** 3,
    "tb": 1024 ** 4,
    "tib": 1024 ** 4,
}


def parse_byte_size(value: str | None) -> int:
    if value is None:
        return 0
    text = str(value).strip().lower().replace("_", "")
    if not text:
        return 0
    match = re.fullmatch(r"(\d+(?:\.\d+)?)\s*([a-z]*)", text)
    if not match:
        raise ValueError(f"Invalid byte size: {value}")
    number, unit = match.groups()
    if unit not in SIZE_UNITS:
        raise ValueError(f"Unknown byte-size unit '{unit}' in {value}")
    return int(float(number) * SIZE_UNITS[unit])


def format_bytes(size: int) -> str:
    value = float(size)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if value < 1024.0 or unit == "TiB":
            return f"{value:.2f} {unit}" if unit != "B" else f"{int(value)} B"
        value /= 1024.0
    return f"{size} B"


def canonical_event_row(event: Event) -> dict:
    return {
        "dataset": event.dataset,
        "entity_id": event.entity_id,
        "event_tick": event.event_tick,
        "source": event.source,
        "event_type": event.event_type,
        "technique": event.technique,
        "evidence_confidence": event.evidence_confidence,
        "threat_intel_confidence": event.threat_intel_confidence,
        "asset_criticality": event.asset_criticality,
        "maintenance_active": event.maintenance_active,
        "malicious": event.malicious,
        "campaign_id": event.campaign_id,
        "host_group": event.host_group,
        "attack_type": event.attack_type,
        "split": event.split,
    }


def canonical_event_line(event: Event) -> str:
    return json.dumps(canonical_event_row(event), sort_keys=True, separators=(",", ":")) + "\n"


def estimate_canonical_corpus_bytes(events: list[Event]) -> int:
    return sum(len(canonical_event_line(event).encode("utf-8")) for event in events)


def write_canonical_corpus(path: Path, events: list[Event], byte_limit: int) -> dict:
    path.parent.mkdir(parents=True, exist_ok=True)
    written_bytes = 0
    written_events = 0
    with path.open("wt", encoding="utf-8", newline="") as handle:
        for event in events:
            line = canonical_event_line(event)
            line_bytes = len(line.encode("utf-8"))
            if byte_limit and written_bytes + line_bytes > byte_limit:
                break
            handle.write(line)
            written_bytes += line_bytes
            written_events += 1
    return {
        "path": str(path),
        "eventCount": written_events,
        "bytes": written_bytes,
        "size": format_bytes(written_bytes),
    }


def split_policy_payload(split: dict[str, list[Window]], preview_limit: int) -> dict:
    def preview(items: list[Window]) -> list[str]:
        selected = items if preview_limit <= 0 else items[:preview_limit]
        return [window.window_id for window in selected]

    return {
        "neverRandomRowSplit": True,
        "axes": ["time_period", "campaign", "host_group", "attack_type"],
        "splitCounts": {name: len(items) for name, items in split.items()},
        "splitPreviewLimit": preview_limit,
        "splitOmittedCounts": {
            name: 0 if preview_limit <= 0 else max(0, len(items) - preview_limit)
            for name, items in split.items()
        },
        "splits": {name: preview(items) for name, items in split.items()},
    }


def split_counts(windows: list[Window]) -> dict[str, int]:
    counts = {"train": 0, "validation": 0, "test": 0}
    for window in windows:
        counts[window.split] = counts.get(window.split, 0) + 1
    return counts


def training_checksum(events: list[Event]) -> str:
    payload = "\n".join(
        f"{event.dataset}|{event.campaign_id}|{event.entity_id}|{event.event_tick}|{event.technique}|{event.malicious}"
        for event in sorted(events, key=lambda e: (e.dataset, e.campaign_id, e.event_tick, e.technique))
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


SECURITY_NEURON_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.neuron.impl.security"
SECURITY_SIGNAL_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.signals.impl.security"
SECURITY_PROCESSOR_PACKAGE = "com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security"
SIMPLE_CHAIN = "com.rakovpublic.jneuropallium.ai.neurons.base.SimpleSignalChain"


def qname(package: str, name: str) -> str:
    return f"{package}.{name}"


def processor(signal: str, processor_class: str, interface: str) -> dict:
    return {
        "signalProcessorClass": qname(SECURITY_PROCESSOR_PACKAGE, processor_class),
        "neuronInterface": qname(SECURITY_NEURON_PACKAGE, interface),
    }


def neuron(neuron_id: int, neuron_class: str, interfaces: list[str], result_classes: list[str],
           processor_map: dict[str, dict], processing_chain: list[str], extra: dict | None = None) -> dict:
    payload = {
        "neuronId": neuron_id,
        "currentNeuronClass": qname(SECURITY_NEURON_PACKAGE, neuron_class),
        "resultClasses": [qname(SECURITY_SIGNAL_PACKAGE, name) for name in result_classes],
        "processorMap": processor_map,
        "mergerMap": {},
        "axon": {
            "connectionMap": {},
            "addressMap": {},
            "connectionsWrapped": False,
            "defaultWeights": {},
        },
        "dendrites": {
            "weights": {},
            "defaultDendritesWeights": {},
        },
        "signalChain": {
            "clazz": SIMPLE_CHAIN,
            "processingChain": processing_chain,
        },
        "isProcessed": False,
        "changed": False,
        "onDelete": False,
        "run": 0,
        "interfaces": [qname(SECURITY_NEURON_PACKAGE, name) for name in interfaces],
    }
    if extra:
        payload.update(extra)
    return payload


def model_layers_summary(artifact: dict) -> list[dict]:
    feature_count = len(artifact["featureNames"])
    return [
        {
            "layerID": 0,
            "file": "layer-0.json",
            "name": "Multi-Source Security Event Input",
            "type": "initInput",
            "size": 0,
            "quantitative": {
                "neurons": 0,
                "canonicalFeatureCount": feature_count,
                "sourceFamilies": len(artifact["dataSources"]),
            },
        },
        {
            "layerID": 1,
            "file": "layer-1-fast-evidence.json",
            "name": "Fast Security Evidence Receptors",
            "type": "fastEvidence",
            "size": 4,
            "quantitative": {
                "neurons": 4,
                "inputSignalTypes": 5,
                "outputSignalTypes": 2,
            },
        },
        {
            "layerID": 2,
            "file": "layer-2-temporal-correlation.json",
            "name": "Trained Temporal Threat Correlator",
            "type": "temporalCorrelation",
            "size": 1,
            "quantitative": {
                "neurons": 1,
                "trainedFeatureWeights": feature_count,
                "decisionThreshold": artifact["decisionThreshold"],
            },
        },
        {
            "layerID": 3,
            "file": "layer-3-response-planning.json",
            "name": "Advisory Response Planning And Safety Gate",
            "type": "responsePlanning",
            "size": 2,
            "quantitative": {
                "neurons": 2,
                "responseBands": len(artifact["responseBands"]),
                "safetyMode": "ADVISORY",
            },
        },
        {
            "layerID": 4,
            "file": "result-layer.json",
            "name": "Security Advisory Result Output",
            "type": "result",
            "size": 1,
            "quantitative": {
                "neurons": 1,
                "resultSignalTypes": 3,
            },
        },
    ]


def signal_frequency_map() -> dict:
    return {
        qname(SECURITY_SIGNAL_PACKAGE, "PacketSignal"): {"epoch": "1", "loop": "1"},
        qname(SECURITY_SIGNAL_PACKAGE, "LogEventSignal"): {"epoch": "1", "loop": "1"},
        qname(SECURITY_SIGNAL_PACKAGE, "SyscallSignal"): {"epoch": "1", "loop": "1"},
        qname(SECURITY_SIGNAL_PACKAGE, "SignatureMatchSignal"): {"epoch": "1", "loop": "1"},
        qname(SECURITY_SIGNAL_PACKAGE, "AnomalyScoreSignal"): {"epoch": "1", "loop": "1"},
        qname(SECURITY_SIGNAL_PACKAGE, "ThreatHypothesisSignal"): {"epoch": "1", "loop": "2"},
        qname(SECURITY_SIGNAL_PACKAGE, "QuarantineRequestSignal"): {"epoch": "1", "loop": "2"},
        qname(SECURITY_SIGNAL_PACKAGE, "IncidentReportSignal"): {"epoch": "1", "loop": "5"},
    }


def write_layer_model_artifacts(output_dir: Path, artifact: dict, descriptor: dict, quantitative: dict) -> None:
    signal = lambda name: qname(SECURITY_SIGNAL_PACKAGE, name)
    feature_weights = dict(zip(artifact["featureNames"], artifact["weights"]))
    layer_0 = {
        "layerID": 0,
        "layerName": "Multi-Source Security Event Input",
        "layerType": "initInput",
        "layerSize": 0,
        "neurons": [],
        "canonicalInputs": {
            "LANL": ["auth", "process", "dns", "flow", "redteam"],
            "ToN_IoT": ["network", "windows", "linux", "zeek", "ground_truth"],
            "OpTC": ["ecar_process", "file", "network", "host", "red_team_ground_truth"],
            "CIC-IDS2017": ["labelled_flow_csv", "pcap"],
            "CSE-CIC-IDS2018": ["labelled_flow_csv", "logs", "pcap"],
            "UNSW-NB15": ["labelled_network_features"],
            "CALDERA": ["sysmon", "auditd", "zeek", "suricata", "ground_truth"],
        },
        "outputSignals": [
            signal("PacketSignal"),
            signal("LogEventSignal"),
            signal("SyscallSignal"),
            signal("SignatureMatchSignal"),
            signal("AnomalyScoreSignal"),
        ],
        "splitPolicy": artifact["splitPolicy"],
        "quantitative": {
            "neurons": 0,
            "trainingEvents": artifact["trainingSummary"]["eventCount"],
            "trainingWindows": artifact["trainingSummary"]["windowCount"],
            "effectiveEvents": artifact["trainingSummary"].get("effectiveEventCount", 0),
            "effectiveWindows": artifact["trainingSummary"].get("effectiveWindowCount", 0),
        },
    }
    layer_1_neurons = [
        neuron(
            0,
            "NetworkFlowNeuron",
            ["INetworkFlowNeuron"],
            [],
            {signal("PacketSignal"): processor("PacketSignal", "PacketFlowProcessor", "INetworkFlowNeuron")},
            [signal("PacketSignal")],
            {"role": "aggregate packet bytes and packet counts into network-flow evidence"},
        ),
        neuron(
            1,
            "SignaturePatternNeuron",
            ["ISignaturePatternNeuron"],
            ["SignatureMatchSignal"],
            {
                signal("PacketSignal"): processor("PacketSignal", "PacketSignatureProcessor", "ISignaturePatternNeuron"),
                signal("LogEventSignal"): processor("LogEventSignal", "LogSignatureProcessor", "ISignaturePatternNeuron"),
            },
            [signal("PacketSignal"), signal("LogEventSignal")],
            {"role": "produce signature evidence from packet and log streams"},
        ),
        neuron(
            2,
            "ProcessBehaviourNeuron",
            ["IProcessBehaviourNeuron"],
            ["AnomalyScoreSignal"],
            {signal("SyscallSignal"): processor("SyscallSignal", "SyscallBehaviourProcessor", "IProcessBehaviourNeuron")},
            [signal("SyscallSignal")],
            {"role": "score process/syscall behavior before temporal correlation"},
        ),
        neuron(
            3,
            "EntityBehaviourBaselineNeuron",
            ["IEntityBehaviourBaselineNeuron"],
            ["SelfToleranceSignal"],
            {
                signal("InflammationBroadcastSignal"): processor(
                    "InflammationBroadcastSignal", "InflammationBaselineProcessor", "IEntityBehaviourBaselineNeuron"
                )
            },
            [signal("InflammationBroadcastSignal")],
            {"role": "track entity baseline state and expose tolerance context"},
        ),
    ]
    layer_1 = {
        "layerID": 1,
        "layerName": "Fast Security Evidence Receptors",
        "layerType": "fastEvidence",
        "layerSize": len(layer_1_neurons),
        "neurons": layer_1_neurons,
        "quantitative": {
            "neurons": len(layer_1_neurons),
            "inputSignalTypes": 5,
            "outputSignalTypes": 3,
        },
    }
    layer_2_neuron = neuron(
        0,
        "TemporalThreatCorrelationNeuron",
        ["ITemporalThreatCorrelationNeuron", "IThreatHypothesisNeuron"],
        ["ThreatHypothesisSignal"],
        {
            signal("AnomalyScoreSignal"): processor("AnomalyScoreSignal", "AnomalyHypothesisProcessor", "IThreatHypothesisNeuron"),
            signal("SignatureMatchSignal"): processor("SignatureMatchSignal", "SignatureHypothesisProcessor", "IThreatHypothesisNeuron"),
        },
        [signal("AnomalyScoreSignal"), signal("SignatureMatchSignal")],
        {
            "trainedTemporalModel": {
                "snapshot": "trained-temporal-threat-model.json",
                "trainedSnapshotVersion": artifact["trainedSnapshotVersion"],
                "featureNames": artifact["featureNames"],
                "scaler": artifact["scaler"],
                "weights": artifact["weights"],
                "bias": artifact["bias"],
                "decisionThreshold": artifact["decisionThreshold"],
                "sequenceGates": artifact["sequenceGates"],
            },
            "dendrites": {
                "weights": feature_weights,
                "defaultDendritesWeights": {},
                "bias": artifact["bias"],
                "decisionThreshold": artifact["decisionThreshold"],
            },
            "baselineAdaptation": {
                "freezeWhenPosteriorAtLeast": 0.30,
                "freezeWhenSignatureConfidenceAtLeast": 0.80,
                "trustedBenignOnly": True,
            },
            "quantitative": {
                "trainedFeatureWeights": len(artifact["weights"]),
                "trainingWindows": artifact["trainingSummary"]["windowCount"],
                "effectiveTrainingWindows": artifact["trainingSummary"].get("effectiveWindowCount", 0),
            },
        },
    )
    layer_2 = {
        "layerID": 2,
        "layerName": "Trained Temporal Threat Correlator",
        "layerType": "temporalCorrelation",
        "layerSize": 1,
        "neurons": [layer_2_neuron],
        "metrics": artifact["metrics"],
        "quantitative": {
            "neurons": 1,
            "trainedFeatureWeights": len(artifact["weights"]),
            "decisionThreshold": artifact["decisionThreshold"],
        },
    }
    layer_3_neurons = [
        neuron(
            0,
            "ResponsePlanningNeuron",
            ["IResponsePlanningNeuron"],
            ["QuarantineRequestSignal"],
            {signal("ThreatHypothesisSignal"): processor("ThreatHypothesisSignal", "HypothesisResponseProcessor", "IResponsePlanningNeuron")},
            [signal("ThreatHypothesisSignal")],
            {
                "responseBands": artifact["responseBands"],
                "safetyMode": "ADVISORY",
                "role": "convert posterior bands into advisory response candidates",
            },
        ),
        neuron(
            1,
            "ResponseGateNeuron",
            ["IResponseGateNeuron"],
            ["QuarantineRequestSignal"],
            {signal("QuarantineRequestSignal"): processor("QuarantineRequestSignal", "QuarantineGateProcessor", "IResponseGateNeuron")},
            [signal("QuarantineRequestSignal")],
            {
                "safetyMode": "ADVISORY",
                "hardGatePolicy": "fixed configuration; model weights cannot bypass hard allow or approval controls",
            },
        ),
    ]
    layer_3 = {
        "layerID": 3,
        "layerName": "Advisory Response Planning And Safety Gate",
        "layerType": "responsePlanning",
        "layerSize": len(layer_3_neurons),
        "neurons": layer_3_neurons,
        "quantitative": {
            "neurons": len(layer_3_neurons),
            "responseBands": len(artifact["responseBands"]),
            "safetyMode": "ADVISORY",
        },
    }
    result_layer = {
        "layerID": 4,
        "layerName": "Security Advisory Result Output",
        "layerType": "result",
        "isResultLayer": True,
        "layerSize": 1,
        "neurons": [
            neuron(
                0,
                "ResponsePlanningNeuron",
                ["IResponsePlanningNeuron"],
                ["ThreatHypothesisSignal", "QuarantineRequestSignal", "IncidentReportSignal"],
                {signal("ThreatHypothesisSignal"): processor("ThreatHypothesisSignal", "HypothesisResponseProcessor", "IResponsePlanningNeuron")},
                [signal("ThreatHypothesisSignal"), signal("QuarantineRequestSignal"), signal("IncidentReportSignal")],
                {
                    "outputSelection": {
                        "mode": "ADVISORY",
                        "emitHypothesis": True,
                        "emitQuarantineCandidate": True,
                        "emitIncidentReport": True,
                    },
                    "quantitative": {
                        "resultSignalTypes": 3,
                    },
                },
            )
        ],
        "quantitative": {
            "neurons": 1,
            "resultSignalTypes": 3,
        },
    }
    trained_update = {
        "modelId": artifact["modelId"],
        "trainedSnapshotVersion": artifact["trainedSnapshotVersion"],
        "updatedAtUtc": artifact["generatedAt"],
        "status": "TRAINED",
        "trainingMode": artifact["trainingMode"],
        "trainingChecksum": artifact["trainingChecksum"],
        "temporalCorrelationLayer": "layer-2-temporal-correlation.json",
        "layers": {
            "temporalCorrelation": {
                "neuronId": 0,
                "featureWeights": feature_weights,
                "bias": artifact["bias"],
                "scaler": artifact["scaler"],
                "decisionThreshold": artifact["decisionThreshold"],
                "sequenceGates": artifact["sequenceGates"],
            }
        },
        "metrics": artifact["metrics"],
        "trainingSummary": artifact["trainingSummary"],
    }
    descriptor.update({
        "modelName": "Temporal Cybersecurity Threat Correlation Network",
        "description": (
            "Jneopallium-style temporal cybersecurity model with explicit fast evidence receptors, "
            "a trained temporal threat-correlation neuron, advisory response planning, and a fixed safety gate."
        ),
        "latestTrainedSnapshot": "trained-model-update.json",
        "latestTrainedTemporalModel": "trained-temporal-threat-model.json",
        "generatedFrom": {
            "codePackage": "com.rakovpublic.jneuropallium.worker.net.neuron.impl.security",
            "generator": "train_temporal_model.py",
            "sourceRuntimeClasses": [
                qname(SECURITY_NEURON_PACKAGE, "NetworkFlowNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "SignaturePatternNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "ProcessBehaviourNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "EntityBehaviourBaselineNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "TemporalThreatCorrelationNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "ResponsePlanningNeuron"),
                qname(SECURITY_NEURON_PACKAGE, "ResponseGateNeuron"),
            ],
        },
        "networkConfig": {
            "safetyMode": "ADVISORY",
            "featureCount": len(artifact["featureNames"]),
            "decisionThreshold": artifact["decisionThreshold"],
            "responseBands": artifact["responseBands"],
            "sequenceGates": artifact["sequenceGates"],
        },
        "layers": model_layers_summary(artifact),
        "signalFrequencyMap": signal_frequency_map(),
        "totalLayers": 5,
        "totalRealNeurons": 8,
        "totalTrainableWeightScalars": len(artifact["weights"]),
        "totalTrainableBiasScalars": 1,
        "notes": [
            "Layer 0 is an explicit multi-source input boundary with no neuron objects.",
            "Layer 2 embeds the trained logistic temporal-correlator weights from trained-temporal-threat-model.json.",
            "Layer 3 remains advisory-only; hard safety gates are fixed configuration and not learned.",
            "Real production accuracy requires external LANL/ToN_IoT/OpTC/CIC/UNSW/CALDERA validation.",
        ],
    })
    quantitative["jneopalliumLayerSummary"] = {
        "layers": model_layers_summary(artifact),
        "totalLayers": 5,
        "totalRealNeurons": 8,
        "totalTrainableWeightScalars": len(artifact["weights"]),
        "totalTrainableBiasScalars": 1,
    }
    write_json(output_dir / "layer-0.json", layer_0)
    write_json(output_dir / "layer-1-fast-evidence.json", layer_1)
    write_json(output_dir / "layer-2-temporal-correlation.json", layer_2)
    write_json(output_dir / "layer-3-response-planning.json", layer_3)
    write_json(output_dir / "result-layer.json", result_layer)
    write_json(output_dir / "trained-model-update.json", trained_update)


def write_artifacts(output_dir: Path, events: list[Event], windows: list[Window], split: dict[str, list[Window]],
                    model: dict, threshold: float, args: argparse.Namespace) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    metrics = {
        name: evaluate(items, model, threshold)
        for name, items in split.items()
    }
    all_metrics = evaluate(windows, model, threshold)
    generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    artifact = {
        "schemaVersion": "1.0",
        "modelId": "cybersecurity-temporal-threat-correlator",
        "trainedSnapshotVersion": "1.0.0-reference-temporal",
        "generatedAt": generated_at,
        "trainingMode": "external-manifest" if args.manifest else "bundled-reference-corpus",
        "trainingChecksum": training_checksum(events),
        "dataSources": SOURCE_IDS,
        "featureNames": FEATURE_NAMES,
        "scaler": {
            "mean": model["means"],
            "std": model["stds"],
        },
        "weights": model["weights"],
        "bias": model["bias"],
        "decisionThreshold": threshold,
        "responseBands": {
            "log": [0.0, 0.30],
            "alert": [0.30, 0.60],
            "connectionQuarantineCandidate": [0.60, 0.85],
            "hostQuarantineCandidate": [0.85, 1.0],
        },
        "sequenceGates": [
            "UNUSUAL_LOGIN -> EXECUTION",
            "EXECUTION -> LATERAL_MOVEMENT",
            "CREDENTIAL_ACCESS -> LATERAL_MOVEMENT",
            "LATERAL_MOVEMENT -> COMMAND_AND_CONTROL",
            "COMMAND_AND_CONTROL -> EXFILTRATION",
        ],
        "splitPolicy": split_policy_payload(split, args.split_preview_limit),
        "trainingSummary": {
            "eventCount": len(events),
            "windowCount": len(windows),
            "positiveWindows": sum(window.label for window in windows),
            "negativeWindows": len(windows) - sum(window.label for window in windows),
            "sourceEventCounts": source_counts(events),
            "referenceMultiplier": args.reference_multiplier if not args.manifest else 1,
            "effectiveReferenceMultiplier": args.effective_reference_multiplier,
            "targetCorpusBytes": args.target_corpus_bytes_value,
            "targetCorpusSize": format_bytes(args.target_corpus_bytes_value)
            if args.target_corpus_bytes_value else "not requested",
            "estimatedCanonicalCorpusBytes": args.estimated_corpus_bytes,
            "estimatedCanonicalCorpusSize": format_bytes(args.estimated_corpus_bytes),
            "estimatedEffectiveCanonicalCorpusBytes": args.estimated_effective_corpus_bytes,
            "estimatedEffectiveCanonicalCorpusSize": format_bytes(args.estimated_effective_corpus_bytes),
            "effectiveCorpusReachRatio": args.effective_corpus_reach_ratio,
            "effectiveEventCount": args.effective_event_count,
            "effectiveWindowCount": args.effective_window_count,
            "effectiveSourceEventCounts": args.effective_source_event_counts,
            "effectiveSplitCounts": args.effective_split_counts,
            "maxCorpusBytes": args.max_corpus_bytes_value,
            "maxCorpusSize": format_bytes(args.max_corpus_bytes_value) if args.max_corpus_bytes_value else "unbounded",
            "exportedCorpus": args.exported_corpus,
            "epochs": args.epochs,
            "maxTrainWindowsPerEpoch": args.max_train_windows_per_epoch,
        },
        "metrics": {
            "train": metrics.get("train", {}),
            "validation": metrics.get("validation", {}),
            "test": metrics.get("test", {}),
            "overall": all_metrics,
            "calibration": calibration(windows, model),
        },
        "deploymentNotes": [
            "Treat this artifact as the checked-in reference model unless retrained with external source manifests.",
            "Keep hard response gates fixed; do not learn allow-list or critical-asset bypass rules.",
            "Freeze baseline adaptation when posterior reaches WATCH or signature confidence is high.",
        ],
    }
    descriptor = {
        "modelId": artifact["modelId"],
        "title": "Temporal Cybersecurity Threat Correlation",
        "version": artifact["trainedSnapshotVersion"],
        "artifact": "trained-temporal-threat-model.json",
        "featureCount": len(FEATURE_NAMES),
        "dataSources": SOURCE_IDS,
        "safetyMode": "ADVISORY",
        "threshold": threshold,
        "metrics": artifact["metrics"],
    }
    quantitative = {
        "modelId": artifact["modelId"],
        "trainingSummary": artifact["trainingSummary"],
        "metrics": artifact["metrics"],
        "topPositiveWeights": top_weights(model["weights"], descending=True),
        "topNegativeWeights": top_weights(model["weights"], descending=False),
    }
    source_mapping = {
        "sources": {
            "LANL": ["AuthenticationEventSignal", "ProcessEventSignal", "DnsLookupSignal", "NetworkFlowSignal"],
            "ToN_IoT": ["AuthenticationEventSignal", "ProcessEventSignal", "DnsLookupSignal", "NetworkFlowSignal"],
            "OpTC": ["ProcessEventSignal", "DnsLookupSignal", "NetworkFlowSignal"],
            "CIC-IDS2017": ["NetworkFlowSignal"],
            "CSE-CIC-IDS2018": ["NetworkFlowSignal"],
            "UNSW-NB15": ["NetworkFlowSignal"],
            "CALDERA": ["AuthenticationEventSignal", "ProcessEventSignal", "DnsLookupSignal", "NetworkFlowSignal"],
        },
        "canonicalEventFields": list(Event.__dataclass_fields__.keys()),
    }
    write_layer_model_artifacts(output_dir, artifact, descriptor, quantitative)
    write_json(output_dir / "trained-temporal-threat-model.json", artifact)
    write_json(output_dir / "model-descriptor.json", descriptor)
    write_json(output_dir / "quantitative-summary.json", quantitative)
    write_json(output_dir / "source-mapping.json", source_mapping)


def top_weights(weights: list[float], descending: bool) -> list[dict]:
    pairs = sorted(zip(FEATURE_NAMES, weights), key=lambda item: item[1], reverse=descending)
    return [{"feature": name, "weight": value} for name, value in pairs[:8]]


def write_json(path: Path, payload: dict) -> None:
    path.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, help="External dataset manifest JSON")
    parser.add_argument("--output-dir", type=Path,
                        default=Path("worker/src/main/resources/model/cybersecurity-temporal"))
    parser.add_argument("--epochs", type=int, default=2400)
    parser.add_argument("--learning-rate", type=float, default=0.45)
    parser.add_argument("--l2", type=float, default=0.01)
    parser.add_argument("--min-test-f1", type=float, default=0.85)
    parser.add_argument("--reference-multiplier", type=int, default=1,
                        help="Deterministically expand the fitted bundled corpus sample by this factor")
    parser.add_argument("--target-corpus-bytes", default="",
                        help="Reference-mode logical corpus target, e.g. 100gb; recorded as effective corpus scale")
    parser.add_argument("--max-corpus-bytes", default="100gb",
                        help="Abort if the effective canonical corpus would exceed this size, e.g. 100gb")
    parser.add_argument("--export-corpus", type=Path,
                        help="Optional path for the expanded canonical JSONL reference corpus")
    parser.add_argument("--max-train-windows-per-epoch", type=int, default=0,
                        help="Deterministic per-epoch training window cap for very large expanded corpora")
    parser.add_argument("--split-preview-limit", type=int, default=500,
                        help="Maximum window IDs per split to write into artifacts; 0 writes all IDs")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    args.max_corpus_bytes_value = parse_byte_size(args.max_corpus_bytes)
    args.target_corpus_bytes_value = parse_byte_size(args.target_corpus_bytes)
    args.exported_corpus = None
    if args.manifest:
        events = external_events(args.manifest)
        args.effective_reference_multiplier = 1
        args.estimated_effective_corpus_bytes = 0
        args.effective_event_count = 0
        args.effective_window_count = 0
        args.effective_source_event_counts = {}
        args.effective_split_counts = {}
    else:
        seed_events = reference_events()
        seed_windows = build_windows(seed_events)
        args.effective_reference_multiplier = args.reference_multiplier
        if args.target_corpus_bytes_value:
            args.effective_reference_multiplier = max(
                args.reference_multiplier,
                reference_multiplier_for_target(
                    seed_events,
                    args.target_corpus_bytes_value,
                    args.max_corpus_bytes_value,
                ),
            )
        args.estimated_effective_corpus_bytes = estimate_reference_corpus_bytes(
            seed_events,
            args.effective_reference_multiplier,
        )
        if (args.max_corpus_bytes_value and
                args.estimated_effective_corpus_bytes > args.max_corpus_bytes_value):
            raise SystemExit(
                f"estimated effective canonical corpus {format_bytes(args.estimated_effective_corpus_bytes)} "
                f"exceeds --max-corpus-bytes {format_bytes(args.max_corpus_bytes_value)}"
            )
        args.effective_event_count = len(seed_events) * args.effective_reference_multiplier
        args.effective_window_count = len(seed_windows) * args.effective_reference_multiplier
        args.effective_source_event_counts = scaled_counts(source_counts(seed_events),
                                                           args.effective_reference_multiplier)
        args.effective_split_counts = scaled_counts(split_counts(seed_windows),
                                                    args.effective_reference_multiplier)
        events = expand_reference_events(seed_events, args.reference_multiplier)
    if not events:
        raise SystemExit("No training events were loaded")
    args.estimated_corpus_bytes = estimate_canonical_corpus_bytes(events)
    if args.max_corpus_bytes_value and args.estimated_corpus_bytes > args.max_corpus_bytes_value:
        raise SystemExit(
            f"estimated canonical corpus {format_bytes(args.estimated_corpus_bytes)} exceeds "
            f"--max-corpus-bytes {format_bytes(args.max_corpus_bytes_value)}"
        )
    if args.export_corpus:
        args.exported_corpus = write_canonical_corpus(
            args.export_corpus, events, args.max_corpus_bytes_value
        )
    windows = build_windows(events)
    split = split_windows(windows)
    if args.manifest:
        args.estimated_effective_corpus_bytes = args.estimated_corpus_bytes
        args.effective_event_count = len(events)
        args.effective_window_count = len(windows)
        args.effective_source_event_counts = source_counts(events)
        args.effective_split_counts = {name: len(items) for name, items in split.items()}
    reach_denominator = args.target_corpus_bytes_value or args.max_corpus_bytes_value
    args.effective_corpus_reach_ratio = (
        args.estimated_effective_corpus_bytes / reach_denominator
        if reach_denominator else 1.0
    )
    model = train_logistic(split["train"], args.epochs, args.learning_rate, args.l2,
                           args.max_train_windows_per_epoch)
    threshold = choose_threshold(split["validation"] or split["train"], model)
    test_metrics = evaluate(split["test"] or windows, model, threshold)
    write_artifacts(args.output_dir, events, windows, split, model, threshold, args)
    print(json.dumps({
        "outputDir": str(args.output_dir),
        "events": len(events),
        "windows": len(windows),
        "threshold": threshold,
        "testF1": test_metrics["f1"],
        "testFalsePositiveRate": test_metrics["falsePositiveRate"],
        "estimatedCanonicalCorpusSize": format_bytes(args.estimated_corpus_bytes),
        "effectiveReferenceMultiplier": args.effective_reference_multiplier,
        "estimatedEffectiveCanonicalCorpusSize": format_bytes(args.estimated_effective_corpus_bytes),
        "maxCorpusSize": format_bytes(args.max_corpus_bytes_value) if args.max_corpus_bytes_value else "unbounded",
    }, indent=2))
    if test_metrics["f1"] < args.min_test_f1:
        raise SystemExit(f"test F1 {test_metrics['f1']:.3f} below required {args.min_test_f1:.3f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
