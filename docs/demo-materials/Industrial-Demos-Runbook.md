# Industrial Demos — Live Runbook

**Two models, one story: supervised control-loop guardianship and label-free predictive maintenance.**
Audience: technical evaluators (reliability / controls / platform engineers).
Everything here is deterministic, runs offline, and is safe to run live.

| Model | What it shows | Data it needs |
|---|---|---|
| **Industrial Loop Guardian** | Supervised diagnosis + advisory optimisation *above* PLC/PID/SIS, with hard safety kept deterministic | An FMI thermal-skid simulation (bundled) |
| **Self-Supervised Maintenance Guardian** | Label-free degradation detection + continuous learning from operator feedback | Ordinary telemetry — **no failure labels** |

The pairing is the pitch: **Loop Guardian** is what you deploy when you have a labelled failure history; **Self-Supervised** is what you deploy when you don't (most sites) — and the two share the same Jneopallium runtime and the same advisory-only safety posture.

---

## 0. Pre-flight (once, before the room)

```bash
# from repo root
cd E:/git/jneopallium

# Python (both demos need only stdlib + PyYAML)
python -V                        # 3.9+
pip install -r scripts/demo-industrial-fmi/requirements.txt   # PyYAML

# JDK 17 + Maven, only for the self-supervised demo (it runs the real Java neurons)
java -version                    # 17
mvn -version
```

Smoke-test both **before** the meeting:

```bash
python scripts/demo-industrial-fmi/run_demo.py pump-wear
bash   scripts/demo-self-supervised-maintenance/demo/run_demo.sh   # or run_demo.ps1 on Windows
```

Keep this runbook and the **Demo-Cheat-Sheet.md** on a second screen.

---

## Part A — Industrial Loop Guardian (supervised)

### A.1 The one-line story
> "It rides *above* your PLC, PID loops, and safety system — diagnosing faults, optimising setpoints, and recommending bounded actions. It never takes over control, and the hard interlock stays deterministic."

### A.2 Run it

```bash
cd scripts/demo-industrial-fmi
python run_demo.py all            # all 9 scenarios; or name one
```

Nine deterministic scenarios: `normal`, `load-disturbance`, `oscillation`, `pump-wear`, `temperature-sensor-drift`, `mqtt-outage`, `opcua-outage`, `high-temperature-interlock`, `operator-override`. Each writes a trace + reports under `target/jneopallium-industrial-fmi/<scenario>/`.

### A.3 What to show and say

**Scenario `pump-wear` — the predictive-maintenance headline.**
```json
"pump-wear": {
  "fault_detection_delay": 58.4,
  "false_positive_count": 0,
  "time_outside_safety_bounds": 0.0,
  "energy_consumption_kwh": 0.53,
  "number_of_actuator_reversals": 88
}
```
> "It flags the developing pump wear **58 seconds** into the fault, with **zero** false positives, and control never leaves the safety envelope."

**Scenario `high-temperature-interlock` — the safety boundary.**
```json
"high-temperature-interlock": {
  "interlock_response_latency": 0.1,
  "time_outside_safety_bounds": 31.9,
  "false_positive_count": 0
}
```
> "When temperature crosses the trip, the **deterministic interlock** responds in **0.1 s** — not the model. Jneopallium advises; the hard safety layer acts. That separation is the whole safety argument."

**Across all nine scenarios:** `false_positive_count = 0` and, apart from the deliberate interlock excursion, `time_outside_safety_bounds = 0.0`.

### A.4 Verify (for the sceptic in the room)
```bash
# the model + runtime are unit-tested
mvn -pl worker -Dtest=IndustrialModuleTest test
```
Point out `target/jneopallium-industrial-fmi/<scenario>/manifest.json` records the exact model file and FMU source used — reproducible evidence, not a slide claim.

---

## Part B — Self-Supervised Maintenance Guardian (label-free)

### B.1 The one-line story
> "Same runtime, but it needs **no failure history**. It learns what 'healthy' looks like from your telemetry, flags drift with a lead time, and gets sharper every shift from a single operator click — while it runs, with no redeploy."

### B.2 Run it

```bash
# (optional) re-fit the label-free model from telemetry — 0 labels
python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py

# the live demo: real Java neurons replaying a telemetry stream
bash scripts/demo-self-supervised-maintenance/demo/run_demo.sh      # Windows: run_demo.ps1
```

Two assets over the same window: **PUMP-101** healthy (with a couple of benign process excursions) and **PUMP-102** with a slow bearing-wear degradation. Fault timings are held in an oracle the model never sees.

### B.3 The transcript to narrate (real output)

```
t=921   ADVISORY  PUMP-101  cavitation     sev=1.80 evidence=1.01  (healthy asset)
        operator -> FALSE POSITIVE. threshold[cavitation] raised to 1.20 (learned live, no redeploy)
t=971   ADVISORY  PUMP-102  bearing_damage sev=1.19 evidence=1.00 lead~30 ticks unc=0.20
        operator -> CONFIRMED. work order raised; threshold[bearing_damage] kept keen at 0.99
t=981   ADVISORY  PUMP-101  bearing_damage sev=2.19 evidence=1.74  (healthy asset)
        operator -> FALSE POSITIVE. threshold[bearing_damage] raised to 1.19 (learned live)
t=1031  ADVISORY  PUMP-102  bearing_damage sev=2.53 evidence=1.93 lead~0 ticks  unc=0.20
        operator -> CONFIRMED. work order raised
...
Real fault (PUMP-102) advisories raised ... 3
Healthy nuisance advisories (before learning) 4
Healthy nuisances suppressed after feedback . 7
Final bearing_damage threshold .............. 1.37 (started 1.00)
Safety posture .............................. ADVISORY (never actuates)
```

### B.4 The three beats to land

1. **Label-free detection.** "PUMP-102 is flagged at **t=971** — roughly **730 ticks before** the modelled failure at t=1700 — and nobody ever labelled a bearing failure. It learned 'normal' from the telemetry and this is a departure from it."
2. **Continuous learning, no redeploy.** "The operator marks the healthy-asset flags **false-positive** with one click; the threshold moves **live** from 1.00 toward 1.37. By the end, **7** nuisance flags are suppressed — and the process never restarted."
3. **Learning doesn't blunt real detection.** "Even as the bearing threshold rises, PUMP-102 keeps firing — its evidence is well above the raised bar. Feedback kills nuisances, not real faults."

### B.5 Verify
```bash
# runtime model (14 tests) and trainer/label-free separation (5 tests)
mvn -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test
python -m unittest -v tests.test_ss_maintenance   # from the demo script dir
```
Call out: the Python suite proves **every** injected fault family separates from the healthy baseline (>2× the p999 extreme) **with zero labels**, and the tests caught two real timestamp-overflow bugs before production.

---

## Part C — Putting the two together (the close)

| Question from the customer | Answer to give |
|---|---|
| "We have years of telemetry but no clean failure log." | Start with **Self-Supervised** — it needs no labels and improves from operator feedback. |
| "We have a labelled maintenance history and tuned loops." | **Loop Guardian** — supervised diagnosis + setpoint optimisation above your control layer. |
| "Will it take over the plant?" | Neither does. Both are **advisory-only**; the hard interlock is deterministic and outside the model (shown live: 0.1 s interlock, model never actuates). |
| "How do we trust it over time?" | Shadow → advisory rollout, feedback drives false positives down, everything is unit-tested and reproducible. |

**Story arc for the room:** show Loop Guardian catching pump wear with the interlock keeping safety deterministic (Part A) → then show Self-Supervised doing predictive maintenance with **no labels at all** and learning from one click (Part B) → close on "same runtime, same safety posture, two entry points depending on the data you have."

---

## Anticipated technical questions

- **"Isn't the self-supervised model just anomaly detection?"** It is degradation detection with trend + change-point + own-history severity + a lead time, and it names a likely fault family — but yes, families are heuristic until confirmed events accumulate. That's why the feedback loop exists; be honest about it.
- **"Synthetic data — what does the accuracy mean?"** Bench separation is not a field detection rate. The claim is: the label-free core provably separates the modelled faults, the runtime logic is correct, and it never actuates. Field accuracy is established in shadow on the customer's data.
- **"How does learning survive a restart?"** Neuron state is persisted to the configured storage; challenger promotions are audited and reversible. No redeploy for threshold learning.
- **"What about a brand-new asset with no history?"** It starts in a high-uncertainty / domain-shift mode (reports 'not sure') and can borrow a baseline from fleet peers — it does not make confident claims cold.
- **"Where does it run?"** On-prem, `local` / HTTP / gRPC; ingests OPC UA and MQTT; emits advisories to JSONL / Kafka / MQTT.

## Reset between runs
```bash
rm -rf target/jneopallium-industrial-fmi target/jneopallium-ss-maintenance-demo
```
Nothing touches the plant, the PLC, or any device. Both demos are read-only and deterministic.
