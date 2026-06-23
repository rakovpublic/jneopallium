# Cybersecurity Temporal Correlation Production Deployment Manual

## Scope

This manual describes how to move Demo 06 from the deterministic
reference trainer toward a production advisory deployment. The runtime
mode remains `ADVISORY`: the system may recommend investigation or
quarantine candidates, but it must not isolate hosts or block traffic
without a separate safety case, approval workflow, and rollback path.

## Production Architecture

```text
multi-source telemetry
  -> canonical event adapters
  -> fast host and network receptors
  -> temporal threat-correlation model
  -> response planner
  -> fixed hard safety gate
  -> advisory output and audit evidence
```

Required production sources:

- Authentication events from Windows, Linux, identity providers, and
  privileged-access systems.
- Process execution and parent-child process telemetry.
- DNS lookups and network-flow summaries.
- Threat-intelligence indicators with timestamps and source confidence.
- Asset criticality and owner context.
- Maintenance windows and approved deployment records.
- Ground-truth attack labels from incident reports, red-team exercises,
  or controlled CALDERA campaigns.

## Canonical Event Contract

Each training row must be canonical JSONL or CSV with these fields:

```text
dataset, entity_id, event_tick, source, event_type, technique,
evidence_confidence, threat_intel_confidence, asset_criticality,
maintenance_active, malicious, campaign_id, host_group, attack_type,
split
```

Rules:

- `event_tick` is event time, not ingestion time.
- `campaign_id` groups a temporal incident or matched benign period.
- `split` must be assigned by time period, campaign, host group, or
  attack type. Never randomize individual rows.
- `maintenance_active` is a soft context feature. It must not delete
  evidence.
- `malicious` is ground truth for training only. Runtime inference must
  not depend on it.

## Training Procedure

Reference scale run:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --reference-multiplier 1000 `
  --target-corpus-bytes 100gb `
  --max-corpus-bytes 100gb `
  --max-train-windows-per-epoch 4096
```

External production run:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --manifest scripts\demo-cybersecurity-training\dataset-manifest-template.json `
  --output-dir worker\src\main\resources\model\cybersecurity-temporal `
  --max-corpus-bytes 100gb
```

LANL + ToN_IoT raw-file run:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --manifest scripts\demo-cybersecurity-training\dataset-manifest-lanl-toniot-template.json `
  --output-dir worker\src\main\resources\model\cybersecurity-temporal `
  --max-corpus-bytes 100gb `
  --max-train-windows-per-epoch 4096
```

Download LANL from `https://csr.lanl.gov/data/cyber1/` and place
`auth.txt.gz`, `proc.txt.gz`, `dns.txt.gz`, `flows.txt.gz`, and
`redteam.txt.gz` under
`scripts/demo-cybersecurity-training/external/lanl/`. Download ToN_IoT
from `https://research.unsw.edu.au/projects/toniot-datasets`, extract
the CSV folders under `scripts/demo-cybersecurity-training/external/toniot/`,
and adjust the manifest globs if your extracted folder names differ.

Production gates before deployment:

- All expected source families are present in `source-mapping.json`.
- Test F1 is at least the configured `--min-test-f1`.
- False-positive rate is within the per-host and per-tenant budget.
- Calibration bins show monotonic risk ordering.
- Mean time to detection is below the incident-response objective.
- Baseline-freeze behavior is tested during attack windows.
- Generated artifacts include checksum, source counts, split policy,
  metrics, and model threshold.

## Packaging

The deployment package consists of:

```text
trained-temporal-threat-model.json
model-descriptor.json
layer-0.json
layer-1-fast-evidence.json
layer-2-temporal-correlation.json
layer-3-response-planning.json
result-layer.json
trained-model-update.json
quantitative-summary.json
source-mapping.json
```

The `layer-*.json` and `result-layer.json` files are the deployed
JNeopallium neuron descriptions. `layer-2-temporal-correlation.json`
contains the trained temporal feature list, scaler, weights, bias,
threshold, gates, and feature-to-dendrite mapping. `trained-model-update.json`
is the update payload for copying the same trained parameters into an
already deployed neuron package.

Store each package with:

- Git commit SHA.
- Training command and manifest checksum.
- Dataset manifest version.
- Generated artifact checksums.
- Approval record for advisory deployment.
- Rollback package identifier.

## Runtime Deployment

1. Deploy adapters that convert telemetry into typed security signals.
2. Load `trained-temporal-threat-model.json` into the worker resources.
3. Start in shadow mode and write advisories to an audit topic or JSONL
   sink without notifying operators.
4. Compare advisories against incident tickets, red-team logs, and
   sampled benign activity.
5. Promote to operator-visible advisory mode only after false-positive
   budgets and latency targets are met.
6. Keep active enforcement disabled unless a separate response-control
   design is approved.

## Monitoring

Track these production metrics:

- Events processed per source per minute.
- Dropped, delayed, and out-of-order events.
- Advisory count per host group and tenant.
- False-positive review rate.
- Confirmed true-positive rate.
- Mean time to detection and mean time to operator acknowledgement.
- Baseline updates accepted, frozen, and rejected.
- Maintenance-window suppression rate.
- Model version and threshold used for every advisory.

Every advisory should preserve evidence lineage:

```text
entity_id, event_tick range, contributing sources, techniques,
posterior, response band, baselineFrozen, modelId, model checksum
```

## Safety And Rollback

- Hard safety gates are fixed configuration, not learned model weights.
- Critical asset allowlists and maintenance windows reduce response
  urgency but do not erase evidence.
- Baseline adaptation must freeze when posterior reaches watch/alert
  state or signature confidence is high.
- Keep the previous model package available for immediate rollback.
- Roll back if false positives exceed budget, source coverage drops, or
  event parsing errors create systematic blind spots.

## Production Limitations

The checked-in reference corpus is synthetic and deterministic. It is
useful for repeatable pipeline evidence and safety-gate regression, but
it does not establish real-world detection quality. Production claims
require external multi-source datasets, representative enterprise
telemetry, controlled red-team/CALDERA campaigns, and documented review
of dataset licensing and data handling.
