# Demo 06 Report: Temporal Cybersecurity Threat Correlation

## Executive Summary

This demo shows advisory temporal cybersecurity correlation over a
Kafka-like event stream. It runs Jneopallium locally through generated
model packaging and configuration, then exports advisory results
through the JSONL aggregator.

The upgraded scenario uses heterogeneous telemetry rather than one IDS
table: authentication, process execution, DNS lookup, network flow,
threat intelligence, asset context, and maintenance-window context.
The safety ceiling remains `ADVISORY`.

## Application Story

The scenario contains three streams:

- `user:backup-service@workstation-17`: unusual login, encoded
  PowerShell, remote authentication fanout, rare DNS, and periodic
  outbound flow.
- `svc:deployment-agent@web-tier`: service-account retries and signed
  deployment activity during an approved maintenance window.
- `host:finance-file-01`: weak repeated outbound transfers that become
  meaningful only after temporal correlation and threat-intelligence
  context.

The demo demonstrates that slower context modifies interpretation
without deleting evidence. Maintenance lowers the score but still
emits auditable observations. Attack-window evidence freezes baseline
learning to avoid poisoning.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-06-cybersecurity-kafka-triage/
```

The run uses the real local worker entry path:

```text
Entry -> Runner -> LocalApplication -> JsonlResultAggregator
```

## Network Structure

Layer configuration:

- Layer 0, size 7: heterogeneous security input.
- Layer 1, size 4: telemetry normalization and fast evidence scoring.
- Layer 2, size 3: temporal threat correlation.
- Layer 3, size 2: advisory investigation planning.

Typed demo signal classes:

- `AuthenticationEventSignal`
- `ProcessEventSignal`
- `DnsLookupSignal`
- `NetworkFlowSignal`
- `ThreatIntelContextSignal`
- `AssetContextSignal`
- `MaintenanceWindowSignal`
- `RiskScoreSignal`
- `ThreatHypothesisSignal`
- `SecurityAdvisorySignal`

Production security module extension:

- `TemporalThreatEvidence`
- `ITemporalThreatCorrelationNeuron`
- `TemporalThreatCorrelationNeuron`

## I/O Logic

Input is deterministic Kafka-like event data. Every row carries:

- `eventTick`
- `source`
- `technique`
- `evidenceConfidence`
- `threatIntelConfidence`
- `assetCriticality`
- `maintenanceActive`
- `trainingSources`
- `evidenceSummary`

Output is advisory JSONL with posterior, impact, sequence confidence,
threat-intelligence gate, maintenance gate, and baseline-freeze state.

## Deterministic Behavior

Expected behavior:

- Ordered attack-chain evidence emits `TEMPORAL_THREAT_ADVISORY`.
- Maintenance traffic emits `CONTEXT_SUPPRESSED_OBSERVATION`.
- Low-and-slow outbound traffic emits `LOW_AND_SLOW_CORRELATION`.
- Attack posterior is greater than benign maintenance posterior.
- Attack-window output marks `baselineFrozen=true`.
- No active blocking result is emitted.

## Safety Ceiling

Mode is `ADVISORY`. The demo does not block users, isolate hosts, or
mutate firewall rules. Active response remains outside this demo unless
explicit allowlists, rate limits, approval workflow, and incident
response audit trails are added.

## Training Design

Production training should not use a single conventional IDS table. The
companion design in
[`cybersecurity-training-design.md`](../cybersecurity-training-design.md)
uses:

- LANL Comprehensive Multi-Source Cybersecurity Events
- ToN_IoT
- DARPA OpTC
- CIC-IDS2017 / CSE-CIC-IDS2018
- UNSW-NB15
- MITRE CALDERA lab output

The demo resource manifest at
`worker/src/test/resources/fullrun/demo-06-cybersecurity-kafka-triage/training-data-sources.yaml`
keeps that source mapping close to the runnable scenario.

The checked-in trained reference model is emitted by:

```text
python scripts/demo-cybersecurity-training/train_temporal_model.py
```

Artifacts:

```text
worker/src/main/resources/model/cybersecurity-temporal/trained-temporal-threat-model.json
worker/src/main/resources/model/cybersecurity-temporal/model-descriptor.json
worker/src/main/resources/model/cybersecurity-temporal/quantitative-summary.json
worker/src/main/resources/model/cybersecurity-temporal/source-mapping.json
```

## Verification Evidence

Default full-run evidence is written to `summary.json` after running:

```text
scripts/demo-fullrun/run_all_fullrun_demos.ps1
```

Behavior assertions include:

- `temporalAttackChainDetected`
- `maintenanceContextSuppressed`
- `lowAndSlowCorrelation`
- `baselineFrozenDuringAttack`
- `allTrainingSourcesReferenced`
- `attackScoreGreaterThanBenign`
- `advisoryOnly`

## Real-Bridge Extension

A real Kafka bridge can replace `SecurityTriageDemoInput`. Keep the
same typed event contract and event-time fields so delayed, replayed,
or out-of-order telemetry can still be correlated by event time rather
than only by processing time.
