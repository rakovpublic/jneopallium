# Demo 06 Cybersecurity Training

This folder trains the temporal threat-correlation model used by the
cybersecurity demo.

Reference training, using the bundled deterministic multi-source corpus:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py
```

External production-style training:

```powershell
python scripts\demo-cybersecurity-training\train_temporal_model.py `
  --manifest scripts\demo-cybersecurity-training\dataset-manifest-template.json `
  --output-dir worker\src\main\resources\model\cybersecurity-temporal
```

The external manifest expects canonical JSONL or CSV rows with fields:

```text
dataset, entity_id, event_tick, source, event_type, technique,
evidence_confidence, threat_intel_confidence, asset_criticality,
maintenance_active, malicious, campaign_id, host_group, attack_type,
split
```

Rows must be split by campaign, host group, time period, or attack type.
Do not randomly split individual events.
