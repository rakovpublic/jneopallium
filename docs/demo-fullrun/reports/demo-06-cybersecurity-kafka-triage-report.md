# Demo 06 Report: Cybersecurity Kafka Event Triage

## Executive Summary

This demo shows advisory cybersecurity triage over a Kafka-like event stream. It runs Jneopallium locally through generated model packaging and generated configuration, then exports advisory results through the JSONL aggregator.

The default full-run verification completed with status `PASS`, processed `120` ticks, and wrote `120` output rows.

## Application Story

The scenario contains two streams: an attack-like endpoint with failed-login bursts and poor source reputation, and a benign service-account retry pattern. The network raises investigation advice for the attack-like stream and dampens the benign pattern.

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

- Layer 0, size 4: security event input.
- Layer 1, size 3: feature layer with anomaly and false-positive dampening.
- Layer 2, size 2: threat hypothesis.
- Layer 3, size 2: result conversion.

Typed signal classes:

- `SecurityEventSignal`
- `ThreatHypothesisSignal`
- `RiskScoreSignal`
- `SecurityAdvisorySignal`

Neuron and processor classes:

- `SecurityTriageNeuron`
- `SecurityTriageResultNeuron`
- `SecurityTriageProcessor`
- `SecurityTriageDemoInput`

## I/O Logic

Input is deterministic Kafka-like event data: auth failure count, source reputation, endpoint criticality, event type, and service-account marker. Output is advisory JSONL, including score attributes and investigation reason.

## Deterministic Behavior

Expected behavior:

- Attack-like login burst emits `SECURITY_ADVISORY`.
- Benign service-account retry emits `DAMPENED_BENIGN_PATTERN`.
- Attack score is greater than benign score.
- No active blocking result is emitted.

## Safety Ceiling

Mode is `ADVISORY`. The demo does not block users, isolate hosts, or mutate firewall rules.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `120`
- Output rows: `120`
- `attackScore`: `0.836`
- `benignScore`: `0.18`
- Assertions passed: local mode, JSONL output exists, aggregator called, attack score greater than benign, attack advisory emitted, advisory-only.

## Real-Bridge Extension

A real Kafka bridge can replace `SecurityTriageDemoInput`. Keep active response outside the demo path unless explicit approval, allowlists, rate limits, and incident-response audit trails are added.
