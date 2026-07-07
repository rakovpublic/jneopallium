# Industrial Demo Materials

Everything needed to run a live demo of the two industrial models for a
**technical audience** — the supervised **Industrial Loop Guardian** and the
label-free **Self-Supervised Maintenance Guardian**.

## What's here

| Artifact | File | Use |
|---|---|---|
| **Live runbook** | [Industrial-Demos-Runbook.md](Industrial-Demos-Runbook.md) | Presenter script: agenda, commands, expected output, talking points, Q&A |
| **One-page cheat-sheet** | [Demo-Cheat-Sheet.md](Demo-Cheat-Sheet.md) | Keep on a second screen: commands + numbers + safety line |
| **Walkthrough deck** | deliverables/Jneopallium-Industrial-Demos-Walkthrough-Deck-EN.{docx,pdf} | Project during the demo |

## Runnable scenarios (produce the live output the materials quote)

**A. Industrial Loop Guardian** (supervised, FMI thermal skid — pure Python):
```bash
python scripts/demo-industrial-fmi/run_demo.py all      # 9 scenarios
python scripts/demo-industrial-fmi/run_demo.py pump-wear # the headline
```

**B. Self-Supervised Maintenance** (label-free, runs the REAL Java neurons):
```bash
python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py   # 0 labels
scripts/demo-self-supervised-maintenance/demo/run_demo.ps1                       # Windows
bash scripts/demo-self-supervised-maintenance/demo/run_demo.sh                   # Unix / Git Bash
```
The self-supervised demo generates a telemetry replay (Python) and runs it
through the deployed model (Java): `demo/make_demo_replay.py` +
`demo/SelfSupervisedMaintenanceDemo.java`.

## Rebuild the deck
```bash
python scripts/demo-cybersecurity-training/docgen/build_demo.py
# then LibreOffice --headless --convert-to pdf --outdir docs/demo-materials/deliverables <docx>
```

## The one thing to remember
Both models are **advisory-only**. In the demo, the only component that *acts*
is the deterministic interlock (0.1 s) — never the AI. Lead with the data-fit
("have labels → Loop Guardian; don't → Self-Supervised"), show detection and
live learning, and close on the shared safety posture.
