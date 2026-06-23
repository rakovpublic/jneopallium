# Advertising Fraud Detection

> Status: implementation for Jneopallium advertising invalid-traffic advisory scoring.
> License: BSD 3-Clause.

## Scope

This module models online-advertising invalid-traffic risk across automated bot
traffic, incentivized or motivated traffic, human click farms, forged or replayed
events, click spam and attribution hijacking, and publisher/app/supply-path
spoofing. The public-data model is advisory-only. It produces risk, invalid,
anomalous, or suspicious traffic evidence; it does not assert that a named person
intentionally committed fraud.

## Reused Jneopallium Pieces

- Typed `ISignal` and per-signal `ProcessingFrequency` scheduling.
- Interface-typed `ISignalProcessor` pattern from the security and industrial
  modules.
- Model bundle layout under `worker/src/main/resources/model/...`.
- Python demo/training workflow style from `scripts/demo-cybersecurity-training`
  and `scripts/demo-industrial-fmi`.
- Java 17/Jackson runtime and JUnit test infrastructure.
- Existing bridge posture: HTTP/Kafka-compatible payloads use canonical events,
  while enforcement remains separated from scoring.

## New Advertising-Specific Pieces

- Canonical versioned advertising event schema with event time, ingest time,
  integrity, behavioural, delayed-quality, and supply-chain fields.
- Domain packages:
  - `com.rakovpublic.jneuropallium.worker.net.signals.impl.adfraud`
  - `com.rakovpublic.jneuropallium.worker.net.neuron.impl.adfraud`
  - `com.rakovpublic.jneuropallium.worker.signalprocessor.impl.adfraud`
  - `com.rakovpublic.jneuropallium.worker.demo.adfraud`
- Deterministic simulator for fraud classes that lack complete public labels.
- Leakage-safe split audit by scenario, campaign/device namespace, and adversarial
  scenario holdout.
- Calibrated multi-label fusion exported as Java-loadable JSON fallback model.
- Advisory-only response gate with candidate financial/enforcement actions never
  executed automatically.

## Canonical Schema

The runtime event object preserves identifiers as pseudonymous strings and keeps
both event and ingestion time. Required fields include event identifiers,
impression/click/session/device/publisher/campaign/supply-chain identifiers,
integrity booleans, behavioural interaction evidence, delayed quality outcomes,
and label-provenance metadata. Raw personal identifiers must be HMACed before
model training, persistence, or logs.

Supported event types:

```text
BID_REQUEST, IMPRESSION, VIEWABLE_IMPRESSION, CLICK, LANDING, INTERACTION,
INSTALL, REGISTRATION, PURCHASE, POSTBACK, REFUND, CHARGEBACK, UNINSTALL,
RETENTION, PAYOUT
```

## Signal Cadence

| Signal Family | Loop / Epoch | Meaning |
|---|---:|---|
| Event integrity, bid, impression, click, postback, decision | 1 / 1 | Fast deterministic evidence |
| User interaction aggregation | 1 / 2 | Behavioural aggregation |
| Session sequence and attribution | 1 / 5 | Causal chain checks |
| Entity baseline | 2 / 1 | Adaptive baseline update |
| Graph cluster update | 2 / 3 | Rolling farm/fanout graph |
| Traffic quality | 2 / 5 | Delayed quality evidence |
| Retention/refund/billing | 2 / 10 | Slow delayed evidence |
| Model drift | 2 / 60 | Batch/monitoring cadence |

Physical tick duration is deployment configuration; these are semantic scheduler
cadences.

## Runtime Neurons

- `EventAuthenticityNeuron` owns deterministic integrity checks.
- `HumanInteractionNeuron` estimates automation without relying only on IP or
  user agent.
- `SessionSequenceNeuron` tracks impression -> click -> landing -> interaction
  -> conversion ordering.
- `AttributionIntegrityNeuron` detects click spam, click injection, duplicated
  conversion and inconsistent attribution evidence.
- `PublisherBaselineNeuron` maintains rolling baselines and freezes adaptation
  when strong fraud evidence is present.
- `ClickFarmGraphNeuron` maintains rolling graph fanout features with TTL.
- `TrafficQualityNeuron` uses delayed retention, refund, chargeback and uninstall
  evidence without treating every non-conversion as fraud.
- `FraudCorrelationNeuron` fuses rules and learned calibrated probabilities.
- `FraudResponseGateNeuron` enforces SHADOW/ADVISORY behaviour and never executes
  consequential action automatically.

## Training Workflow

`scripts/demo-ad-fraud/run_all.*` performs source discovery, bounded automatic
download/cache where legally and technically possible, deterministic scenario
generation, normalization, leakage audit, baseline training, calibration,
evaluation, model export, Java tests, demo replay, readiness reporting, and final
documentation. Unavailable optional sources are recorded as
`dataset_unavailable`, `license_blocked`, or `credentials_required`; the workflow
does not ask for manual files.

## Readiness

The generated report may set `ENGINEERING_READY`, `SHADOW_READY`, and
`ADVISORY_READY` true for the deterministic reference workflow. It must keep
`AUTOMATED_ACTION_READY=false` until first-party labelled production traffic,
forward-time and unseen-publisher validation, sample-size gates, false-positive
financial-cost limits, legal review, rollback, and appeal processes are recorded.
