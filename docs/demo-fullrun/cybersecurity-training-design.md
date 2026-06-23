# Cybersecurity Demo Training Design

## Goal

Demo 06 is a temporal threat-correlation demo, not a single-row IDS
classifier. Production-grade training must learn relationships across
timestamped event streams:

```text
unusual authentication -> process execution -> DNS lookup -> lateral movement -> exfiltration
```

The production target is a set of cooperating components:

- Fast network detector.
- Authentication baseline.
- Process anomaly detector.
- Temporal threat correlator.
- Attack memory.
- Advisory response planner.
- Fixed hard safety gate.
- Baseline adaptation that learns only from trusted benign periods.

## Data Sources

| Source | Use | Streams |
|---|---|---|
| LANL Comprehensive Multi-Source Cybersecurity Events | Primary enterprise temporal validation | Authentication, process start/stop, DNS, network flow, red-team labels |
| ToN_IoT | First complete multi-source implementation set | Windows, Linux, network, Zeek, IoT/IIoT, ground truth |
| DARPA OpTC | Advanced endpoint and APT provenance validation | eCAR endpoint events, process, file, network, host, red-team labels |
| CIC-IDS2017 / CSE-CIC-IDS2018 | Fast network receptor pretraining | PCAP and labelled network-flow CSV |
| UNSW-NB15 | Quick network classifier and throughput validation | Labelled network features and attack categories |
| MITRE CALDERA lab output | Exact labels for controlled temporal attack chains | Sysmon, auditd, Zeek/Suricata, Kafka JSONL, CALDERA ground truth |

Do not distribute a pretrained commercial model that depends on
UNSW-NB15 without resolving author permission. Keep dataset licenses
and download instructions outside generated artifacts.

## Signal Mapping

```text
LANL auth.txt.gz       -> AuthenticationEventSignal
LANL proc.txt.gz       -> ProcessEventSignal
LANL dns.txt.gz        -> DnsLookupSignal
LANL flows.txt.gz      -> NetworkFlowSignal
LANL redteam.txt.gz    -> AttackGroundTruthSignal

ToN_IoT Windows/Linux  -> AuthenticationEventSignal, ProcessEventSignal
ToN_IoT network/Zeek   -> DnsLookupSignal, NetworkFlowSignal

OpTC eCAR process/file -> ProcessEventSignal, FileOperationSignal
OpTC network           -> NetworkFlowSignal

CIC/CSE-CIC flows      -> AnomalyScoreSignal for network evidence
UNSW-NB15 rows         -> AnomalyScoreSignal for network evidence

CALDERA abilities      -> AttackTechniqueSignal, AttackGroundTruthSignal
```

## Training Plan

The executable trainer lives at:

```text
scripts/demo-cybersecurity-training/train_temporal_model.py
```

Reference training command:

```text
python scripts/demo-cybersecurity-training/train_temporal_model.py
```

Production-scale reference evidence command:

```text
python scripts/demo-cybersecurity-training/train_temporal_model.py --reference-multiplier 1000 --target-corpus-bytes 100gb --max-corpus-bytes 100gb --max-train-windows-per-epoch 4096
```

External dataset training command:

```text
python scripts/demo-cybersecurity-training/train_temporal_model.py --manifest scripts/demo-cybersecurity-training/dataset-manifest-template.json
```

The trainer emits:

```text
worker/src/main/resources/model/cybersecurity-temporal/trained-temporal-threat-model.json
worker/src/main/resources/model/cybersecurity-temporal/model-descriptor.json
worker/src/main/resources/model/cybersecurity-temporal/quantitative-summary.json
worker/src/main/resources/model/cybersecurity-temporal/source-mapping.json
```

Production deployment guidance lives in
[`cybersecurity-production-deployment-manual.md`](cybersecurity-production-deployment-manual.md).
The latest production-scale reference evidence is summarized in
[`reports/demo-06-cybersecurity-production-training-evidence.md`](reports/demo-06-cybersecurity-production-training-evidence.md).

Phase 1: pipeline implementation with ToN_IoT.

- Build stream adapters for Windows, Linux, network-flow CSV, Zeek, and
  ground-truth timestamps.
- Train the fast network detector and host-event detector.
- Verify event-time ordering and out-of-order replay handling.

Phase 2: realistic enterprise validation with LANL.

- Stream compressed files instead of loading them into memory.
- Extract attack windows from 30 minutes before a red-team event through
  60 minutes after it.
- Match benign windows by time of day, user/host activity, and absence
  of nearby red-team activity.
- Validate user and host baselines, unusual authentication, lateral
  movement, and false-positive rate per host/day.

Phase 3: advanced APT validation with OpTC.

- Start with one attack day, one benign day, 20 to 50 hosts, and only
  required eCAR event types.
- Validate process/network provenance, delayed evidence, out-of-order
  evidence, and long attack chains.

Phase 4: network detector hardening with CIC/CSE-CIC and UNSW-NB15.

- Train flow-level anomaly and attack-family models.
- Export calibrated `AnomalyScoreSignal` values.
- Treat these scores as receptor evidence, not final incident truth.

Phase 5: exact labels with CALDERA.

- Run isolated lab scenarios for normal administration, maintenance,
  password spraying, PowerShell execution, credential access, lateral
  movement, C2-like beaconing, and simulated exfiltration.
- Emit exact campaign, entity, event time, ATT&CK technique, stage, and
  malicious labels.

## Split Policy

Never split by random individual rows. That leaks near-duplicates from
the same campaign into train and test.

Split by:

- Time period.
- Campaign.
- Host group.
- Attack type.

Recommended structure:

```text
Training:
  benign days 1-20
  campaigns A-D

Validation:
  later time period
  campaigns E-F

Test:
  unseen hosts
  later time period
  at least one unseen attack type
```

## Windows

```text
fast window:       1-10 seconds
behavior window:   1-5 minutes
incident window:   30-120 minutes
baseline window:   hours to days
```

## Component Training

| Component | Training approach |
|---|---|
| Fast network detector | Supervised classifier or autoencoder |
| Authentication baseline | EWMA/statistical or unsupervised model |
| Process anomaly detector | Sequence or frequency model |
| Temporal threat correlator | Explicit event gates first; later GRU/TCN/transformer or Bayesian update |
| Attack memory | Similarity search over incident sequences |
| Response planner | Calibrated thresholds and policy rules |
| Hard safety gate | Fixed configuration, never learned |
| Baseline adaptation | Online learning from trusted benign periods only |

The checked-in reference model currently uses class-balanced logistic
training over temporal-window features. This is intentional: it gives a
transparent, reproducible baseline before larger GRU/TCN/transformer
experiments. The artifact records feature names, standardization
parameters, weights, decision threshold, response bands, source counts,
split membership, and calibration bins.

## Metrics

- True-positive rate.
- False-positive rate.
- Mean time to detection.
- Mean time to containment recommendation.
- Attack-chain completion before detection.
- Baseline poisoning after replay.
- Alerts per incident.
- Processor calls per second.
- Incorrect quarantine recommendations.

## Production Gates

Before claiming production-grade results, require:

- Cross-source validation on ToN_IoT, LANL, OpTC, CIC/CSE-CIC,
  UNSW-NB15, and controlled CALDERA output.
- Calibration curves for detector scores and temporal posteriors.
- Per-entity false-positive budgets.
- Backpressure and streaming-memory tests.
- Evidence lineage from every advisory to source events and event time.
- Baseline-freeze tests during attack windows.
- Advisory-only mode by default; enforcing mode requires a separate
  safety case.
