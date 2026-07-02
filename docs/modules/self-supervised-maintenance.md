# Self-Supervised Maintenance

> Status: implementation for Jneopallium label-free predictive maintenance with
> continuous online learning from operator feedback.
> License: BSD 3-Clause.

## Scope

This module detects "maintenance is required" conditions on rotating assets
(pumps, motors, skids) **without any fault labels**. It learns each asset's
healthy normal from ordinary operating telemetry and from the fleet, flags
degradation via self-supervised objectives, and keeps learning from operator
feedback while it runs. It is advisory-only: it recommends inspection with a
lead time and never actuates a device.

It complements the [industrial](industrial.md) Loop Guardian: that model is
supervised (labelled finding heads); this one is the label-free path for sites
that have telemetry but no maintenance-labelled history yet.

## Reused Jneopallium Pieces

- Typed `ISignal` with per-signal `ProcessingFrequency` (fast telemetry loop,
  slow feedback loop).
- Interface-typed `ISignalProcessor` pattern from the industrial module.
- `ModulatableNeuron` base; model-bundle layout under
  `worker/src/main/resources/model/...`.

## Label-Free Learning

The model uses no labels because every training target is the data itself:

- **Cross-sensor reconstruction** — each sensor is predicted from the others
  (regime-standardised) by a ridge model fitted on a trusted-healthy window; the
  residual is the anomaly signal, and a large single-sensor residual is a sensor
  fault. (`CrossSensorReconstructionNeuron`)
- **Trend + change-point** — a slow EWMA level/slope of the residual with a
  Page-Hinkley detector; the slope extrapolates a lead time to the baseline
  limit. (`MaintenanceHypothesisNeuron`)
- **Own-history severity calibration** — severity is expressed in the asset's
  own healthy-window percentile units, not against labels.
- **Evidence accumulation** — an advisory is raised only when the deviation is
  persistent, trending, consistent across sensors, and not a domain shift.

Fault families (bearing / cavitation / sensor / energy / oscillation / unknown)
are attributed heuristically from which sensors dominate the residual.

## Continuous Learning Without Redeploy

`FeedbackAdaptationNeuron` consumes `OperatorFeedbackSignal` and moves per-family
thresholds: a false positive raises the threshold, a confirmed need relaxes it
slightly. Every update is **bounded** (clamped offset), **rate-limited**, and
**frozen during domain shift** (anti-poisoning). It emits a
`ThresholdUpdateSignal` that `SsAdvisoryGateNeuron` applies to its live threshold
map in place — the deployed model keeps improving without a rebuild or restart,
and the state is persisted to `IStorage` so it survives restarts.

## Network

| Layer | Neuron | In → Out |
|---|---|---|
| 0 | (telemetry ingest) | `AssetTelemetrySignal` |
| 1 | `CrossSensorReconstructionNeuron` | telemetry → `ReconResidualSignal` |
| 2 | `MaintenanceHypothesisNeuron` | residual → `HealthHypothesisSignal` |
| 3 | `FeedbackAdaptationNeuron` | `OperatorFeedbackSignal` → `ThresholdUpdateSignal` |
| 4 | `SsAdvisoryGateNeuron` | hypothesis (+ threshold update) → `MaintenanceAdvisorySignal` |

## Safety Invariants

- `SsMaintConfig.advisoryOnly` cannot be disabled; no neuron actuates.
- Threshold learning never touches a hard safety gate or interlock.
- Domain-shift and uncertainty are first-class outputs so the model can say
  "not sure" on an unfamiliar asset rather than over-claim.

## Training and Tests

- Initial fitting (label-free): `scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py`
  (pure standard library) writes the deployable bundle under
  `model/self-supervised-maintenance/`.
- Runtime tests (Java):
  `worker/.../net/neuron/impl/ssmaint/SelfSupervisedMaintenanceModuleTest`.
- Trainer tests (Python):
  `scripts/demo-self-supervised-maintenance/tests/test_ss_maintenance.py`.

## Honest Limits

Label-free mode detects degradation, not a contractually-due repair; families
are heuristic until weak labels accumulate, so expect more false positives than
a label-trained model (the feedback loop drives them down). A representative
healthy window (or fleet peers) is required to define normal.
