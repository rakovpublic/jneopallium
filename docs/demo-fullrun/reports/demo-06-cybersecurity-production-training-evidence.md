# Demo 06 Production-Scale Training Evidence

## Run Command

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --reference-multiplier 1000 `
  --target-corpus-bytes 100gb `
  --max-corpus-bytes 100gb `
  --max-train-windows-per-epoch 4096
```

## Corpus Scale

The run used the bundled multi-source reference corpus as a deterministic
fitted sample and recorded a production-scale logical corpus target.

| Field | Value |
|---|---:|
| Fitted reference multiplier | 1,000 |
| Fitted events | 96,000 |
| Fitted temporal windows | 21,000 |
| Estimated fitted canonical corpus | 41.45 MiB |
| Effective reference multiplier | 2,411,186 |
| Effective events | 231,473,856 |
| Effective temporal windows | 50,634,906 |
| Target corpus size | 100.00 GiB |
| Estimated effective canonical corpus | 100.00 GiB |
| Target reach ratio | 0.999968588 |

Effective split counts:

| Split | Windows |
|---|---:|
| Training | 24,111,860 |
| Validation | 14,467,116 |
| Test | 12,055,930 |

Effective source event counts:

| Source | Events |
|---|---:|
| LANL | 50,634,906 |
| ToN_IoT | 28,934,232 |
| OpTC | 74,746,766 |
| CIC-IDS2017 | 7,233,558 |
| CSE-CIC-IDS2018 | 7,233,558 |
| UNSW-NB15 | 14,467,116 |
| CALDERA | 48,223,720 |

## Model Metrics

Held-out fitted-sample test metrics:

| Metric | Value |
|---|---:|
| Test windows | 5,000 |
| True positives | 3,000 |
| False positives | 0 |
| True negatives | 2,000 |
| False negatives | 0 |
| Precision | 1.0 |
| Recall | 1.0 |
| F1 | 1.0 |
| False-positive rate | 0.0 |
| Mean time to detection ticks | 0.0 |

## Evidence Artifacts

Generated model artifacts:

```text
worker/src/main/resources/model/cybersecurity-temporal/trained-temporal-threat-model.json
worker/src/main/resources/model/cybersecurity-temporal/model-descriptor.json
worker/src/main/resources/model/cybersecurity-temporal/layer-0.json
worker/src/main/resources/model/cybersecurity-temporal/layer-1-fast-evidence.json
worker/src/main/resources/model/cybersecurity-temporal/layer-2-temporal-correlation.json
worker/src/main/resources/model/cybersecurity-temporal/layer-3-response-planning.json
worker/src/main/resources/model/cybersecurity-temporal/result-layer.json
worker/src/main/resources/model/cybersecurity-temporal/trained-model-update.json
worker/src/main/resources/model/cybersecurity-temporal/quantitative-summary.json
worker/src/main/resources/model/cybersecurity-temporal/source-mapping.json
```

The generated layer files follow the same deployed model-config shape as
the UAV FPV layer JSONs: every neuron declares `layerID`, `neuronID`,
`currentNeuronClass`, processor chain, dendrites, axons, signal classes,
and training metadata. The cybersecurity temporal-correlation neuron
stores the fitted weights directly in
`layer-2-temporal-correlation.json`.

Verification performed:

- Python syntax compile passed for `train_temporal_model.py`.
- Training pipeline completed with F1 above the configured `0.85` gate.
- JSON artifact validation confirmed the 100 GiB target, fitted counts,
  effective counts, and held-out test false-positive rate.
- No 100 GiB corpus file was exported; the run recorded an estimated
  effective corpus so the repository is not inflated by generated data.

## Production Interpretation

This is production-scale pipeline evidence, not production accuracy
evidence. The 100 GiB target is reached by deterministic reference
replication so the trainer can exercise scaling metadata and guardrails
on a desktop. Real production validation must replace this with external
canonical manifests for LANL, ToN_IoT, OpTC, CIC/CSE-CIC, UNSW-NB15, and
controlled CALDERA output.
