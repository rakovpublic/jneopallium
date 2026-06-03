# Demo 05 Report: DICOM Read-Only Context Bridge

## Executive Summary

This demo shows read-only interpretation of DICOM-like study metadata. It runs through the full local Jneopallium path and produces routing and quality-control advisory output without pixel diagnosis or writeback.

The default full-run verification completed with status `PASS`, processed `60` ticks, and wrote `60` output rows.

## Application Story

The scenario compares a complete imaging study with a study missing accession and study metadata. The network flags missing metadata for quality-control review and routes complete metadata to standard review.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-05-dicom-readonly-context/
```

The worker is started through `Entry` with local mode, generated model JAR URL/path, `DemoJsonContext`, and generated context JSON.

## Network Structure

Layer configuration:

- Layer 0, size 3: imaging metadata input.
- Layer 1, size 2: context feature extraction.
- Layer 2, size 2: result conversion.

Typed signal classes:

- `DicomMetadataSignal`
- `ImageQcSignal`
- `RoutingAdvisorySignal`

Neuron and processor classes:

- `DicomContextNeuron`
- `DicomContextResultNeuron`
- `DicomContextProcessor`
- `DicomContextDemoInput`

## I/O Logic

Input is deterministic metadata only: modality, body part, series count, study age, accession presence, and study presence. Output is JSONL read-only advisory output.

## Deterministic Behavior

Expected behavior:

- Complete metadata emits `ROUTING_ADVISORY`.
- Missing accession or study metadata emits `QC_ADVISORY`.
- No command or writeback result is emitted.

## Safety Ceiling

Mode is `READ-ONLY`. The demo does not inspect pixels, diagnose images, or write back to a PACS/RIS.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `60`
- Output rows: `60`
- Assertions passed: local mode, JSONL output exists, aggregator called, read-only mode, missing metadata QC advisory, no command or writeback.

## Real-Bridge Extension

A real DICOM bridge can replace the mock metadata input. Keep the first deployment read-only, use metadata allowlists, and route output to review queues rather than writeback channels.
