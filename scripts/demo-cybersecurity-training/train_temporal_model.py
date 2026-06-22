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
import statistics
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


def row_event(row: dict[str, str], source: dict) -> Event:
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
        for raw_path in paths:
            path = (manifest_path.parent / raw_path).resolve()
            if not path.exists():
                if manifest.get("requireAllSources", False):
                    raise FileNotFoundError(path)
                continue
            for row in read_rows(path):
                events.append(row_event(row, source))
    return events


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


def train_logistic(windows: list[Window], epochs: int, learning_rate: float, l2: float) -> dict:
    means, stds = standardizer(windows)
    weights = [0.0] * len(FEATURE_NAMES)
    bias = 0.0
    positives = sum(window.label for window in windows)
    negatives = len(windows) - positives
    pos_weight = len(windows) / max(1, 2 * positives)
    neg_weight = len(windows) / max(1, 2 * negatives)
    for epoch in range(epochs):
        grad_w = [0.0] * len(weights)
        grad_b = 0.0
        lr = learning_rate / (1.0 + epoch / max(1, epochs))
        for window in windows:
            x = scale(window.features, means, stds)
            y = float(window.label)
            weight = pos_weight if window.label else neg_weight
            prediction = sigmoid(bias + sum(w * v for w, v in zip(weights, x)))
            error = (prediction - y) * weight
            grad_b += error
            for i, value in enumerate(x):
                grad_w[i] += error * value + l2 * weights[i]
        n = max(1, len(windows))
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


def training_checksum(events: list[Event]) -> str:
    payload = "\n".join(
        f"{event.dataset}|{event.campaign_id}|{event.entity_id}|{event.event_tick}|{event.technique}|{event.malicious}"
        for event in sorted(events, key=lambda e: (e.dataset, e.campaign_id, e.event_tick, e.technique))
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


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
        "splitPolicy": {
            "neverRandomRowSplit": True,
            "axes": ["time_period", "campaign", "host_group", "attack_type"],
            "splits": {name: [window.window_id for window in items] for name, items in split.items()},
        },
        "trainingSummary": {
            "eventCount": len(events),
            "windowCount": len(windows),
            "positiveWindows": sum(window.label for window in windows),
            "negativeWindows": len(windows) - sum(window.label for window in windows),
            "sourceEventCounts": source_counts(events),
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
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    events = external_events(args.manifest) if args.manifest else reference_events()
    if not events:
        raise SystemExit("No training events were loaded")
    windows = build_windows(events)
    split = split_windows(windows)
    model = train_logistic(split["train"], args.epochs, args.learning_rate, args.l2)
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
    }, indent=2))
    if test_metrics["f1"] < args.min_test_f1:
        raise SystemExit(f"test F1 {test_metrics['f1']:.3f} below required {args.min_test_f1:.3f}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
