# Industrial Demos — One-Page Cheat-Sheet

Keep this on screen. Full detail in **Industrial-Demos-Runbook.md**.

## The 20-second framing
> Two industrial models, one runtime, one safety posture. **Loop Guardian** = supervised, for sites *with* a labelled failure history. **Self-Supervised** = label-free, for sites *without* one (most of them). Both are **advisory-only** — the hard interlock stays deterministic.

## Commands (copy-paste)

```bash
# A. Industrial Loop Guardian (supervised, FMI skid)
python scripts/demo-industrial-fmi/run_demo.py all
python scripts/demo-industrial-fmi/run_demo.py pump-wear     # the headline
mvn -pl worker -Dtest=IndustrialModuleTest test              # verify

# B. Self-Supervised Maintenance (label-free, real Java neurons)
python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py   # 0 labels
scripts/demo-self-supervised-maintenance/demo/run_demo.ps1   # or run_demo.sh
mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test               # verify (14)
python -m unittest tests.test_ss_maintenance                                 # verify (5)
```

## Numbers to quote (real, from the runs)

| Loop Guardian | Value |
|---|---|
| Pump-wear fault detection delay | **58.4 s** |
| False positives (all 9 scenarios) | **0** |
| Time outside safety bounds (non-interlock) | **0.0** |
| Interlock response latency | **0.1 s** (deterministic, not the model) |

| Self-Supervised | Value |
|---|---|
| Failure labels used | **0** |
| Real fault detected | **t=971**, ~730 ticks before modelled failure (t=1700) |
| Nuisance flags suppressed by feedback | **7** |
| Bearing threshold learned live | **1.00 → 1.37** (no redeploy) |
| Fault families separated without labels | **5 / 5** (>2× p999) |
| Tests passing | **14 Java + 5 Python** |

## Three beats for the self-supervised transcript
1. **Detects with no labels** — learned 'normal', flagged the drift ~730 ticks early.
2. **Learns live from one click** — operator marks false-positive → threshold moves 1.00→1.37, no restart.
3. **Learning ≠ going blind** — real fault still fires above the raised bar; only nuisances die.

## If pushed
- *"Just anomaly detection?"* → trend + change-point + own-history severity + lead time + named family (heuristic until confirmations).
- *"Synthetic accuracy?"* → bench separation ≠ field rate; prove field accuracy in shadow on their data.
- *"Will it control the plant?"* → No. Advisory-only invariant in code; interlock is deterministic and outside the model.

## Safety line (say it at least once)
> "Both models only ever advise. The one that acted in the demo was the **deterministic interlock**, in 0.1 s — not the AI."

## Reset
```bash
rm -rf target/jneopallium-industrial-fmi target/jneopallium-ss-maintenance-demo
```
