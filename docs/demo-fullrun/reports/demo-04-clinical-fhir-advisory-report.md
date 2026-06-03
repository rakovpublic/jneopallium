# Demo 04 Report: Clinical FHIR Advisory

## Executive Summary

This demo shows a regulatory-safe clinical decision support application using synthetic FHIR-like observations. It is a full local Jneopallium run with generated model JAR, generated context JSON, generated layer metadata, and JSONL output aggregation.

The default full-run verification completed with status `PASS`, processed `80` ticks, and wrote `80` output rows.

## Application Story

The scenario includes a stable synthetic patient and a high-risk synthetic patient. The high-risk patient has abnormal heart rate, oxygen saturation, temperature, and medication context. The network emits clinician-review advisory output and audit reasons, but never emits treatment orders or autonomous writes.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-04-clinical-fhir-advisory/
```

The demo runs via:

```text
Entry local file:///.../demo-model.jar DemoJsonContext context.json
```

## Network Structure

Layer configuration:

- Layer 0, size 4: patient observation input.
- Layer 1, size 3: vitals and trend feature processing.
- Layer 2, size 2: clinical advisory processing.
- Layer 3, size 2: result conversion.

Typed signal classes:

- `VitalSignal`
- `MedicationContextSignal`
- `ClinicalRiskSignal`
- `ClinicalAdvisorySignal`

Neuron and processor classes:

- `ClinicalFhirNeuron`
- `ClinicalFhirResultNeuron`
- `ClinicalFhirProcessor`
- `ClinicalFhirDemoInput`

## I/O Logic

Input is deterministic synthetic patient data. The output aggregator writes advisory cards with result type, decision, reason, confidence, and `ADVISORY` mode.

## Deterministic Behavior

Expected behavior:

- Stable patient output remains observation-only.
- High-risk patient emits `CLINICIAN_REVIEW_ADVISORY`.
- No result type or decision includes autonomous order/write semantics.

## Safety Ceiling

Mode is `ADVISORY`. The demo never writes treatment orders and requires clinician review.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `80`
- Output rows: `80`
- Assertions passed: local mode, JSONL output exists, aggregator called, advisory mode, no autonomous order or write, high-risk patient advisory.

## Real-Bridge Extension

A real FHIR bridge can replace the synthetic input. Keep the output as advisory cards, add patient pseudonymization and policy checks, and keep treatment-order writes outside the demo model.
