# Demo 06 Cybersecurity Training

This folder trains the temporal threat-correlation model used by the
cybersecurity demo.

Reference training, using the bundled deterministic multi-source corpus:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py
```

Expanded reference training, using 1000 deterministic campaign variants
with a 100 GiB effective-corpus target and guardrail:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --reference-multiplier 1000 `
  --target-corpus-bytes 100gb `
  --max-corpus-bytes 100gb `
  --max-train-windows-per-epoch 4096
```

External production-style training:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --manifest scripts\demo-cybersecurity-training\dataset-manifest-template.json `
  --output-dir worker\src\main\resources\model\cybersecurity-temporal
```

LANL + ToN_IoT raw-file training:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --manifest scripts\demo-cybersecurity-training\dataset-manifest-lanl-toniot-template.json `
  --output-dir worker\src\main\resources\model\cybersecurity-temporal `
  --max-corpus-bytes 100gb `
  --max-train-windows-per-epoch 4096
```

Place downloaded files under:

```text
scripts/demo-cybersecurity-training/external/lanl/
  auth.txt.gz
  proc.txt.gz
  dns.txt.gz
  flows.txt.gz
  redteam.txt.gz

scripts/demo-cybersecurity-training/external/toniot/
  ... extracted ToN_IoT CSV folders ...
```

Official source pages:

- LANL Comprehensive Multi-Source Cyber-Security Events:
  https://csr.lanl.gov/data/cyber1/
- ToN_IoT datasets:
  https://research.unsw.edu.au/projects/toniot-datasets

The external manifest expects canonical JSONL or CSV rows with fields:

```text
dataset, entity_id, event_tick, source, event_type, technique,
evidence_confidence, threat_intel_confidence, asset_criticality,
maintenance_active, malicious, campaign_id, host_group, attack_type,
split
```

Rows must be split by campaign, host group, time period, or attack type.
Do not randomly split individual events.

The `--max-corpus-bytes` value is a safety guardrail for the canonical
expanded corpus. The `--target-corpus-bytes` value records the logical
production-scale corpus size reached by deterministic reference
replication while the fitted sample remains bounded for a desktop run.
Use external manifests for LANL, ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15,
or CALDERA-scale corpora before claiming real production accuracy.

The trainer emits the compact temporal model plus JNeopallium-style
layer/neuron config JSONs in the output directory:

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

`layer-2-temporal-correlation.json` embeds the trained feature names,
scaler, weights, bias, threshold, temporal gates, and dendrite weights.

The LANL + ToN_IoT template also accepts raw LANL files (`lanl-auth`,
`lanl-proc`, `lanl-dns`, `lanl-flow`, `lanl-redteam`) and common
ToN_IoT CSV files (`toniot-network`, `toniot-windows`, `toniot-linux`).
Tune `maxRows`, `startTick`, `endTick`, and `sampleModulo` in the
manifest before training on large extracts.
