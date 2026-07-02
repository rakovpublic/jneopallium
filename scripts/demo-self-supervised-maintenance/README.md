# Self-Supervised Maintenance Guardian (demo)

Label-free predictive maintenance with **continuous online learning from
operator feedback**, built as a real Jneopallium model.

The model learns "maintenance is required" **without any fault labels**. Each
sensor is predicted from the others (regime-standardised); the reconstruction
residual is the anomaly signal. A fusion neuron turns the residual stream into a
trend / change-point / severity / evidence hypothesis with an estimated lead
time. Operator feedback then adapts the decision thresholds **while the process
runs — no redeploy** — and the whole model is advisory: it never actuates.

## Where the logic lives

| Concern | Where |
|---|---|
| Runtime model (neurons, signals, processors) | **Java** — `worker/.../impl/ssmaint` |
| Deployable network (layers + descriptor + context) | `worker/src/main/resources/model/self-supervised-maintenance/` |
| Initial (label-free) training that fits the params | **Python** — `train_ss_maintenance_model.py` |
| Tests | **Java** `SelfSupervisedMaintenanceModuleTest`, **Python** `tests/` |

Python is used **only** for initial training and tests. All detection and
learning happens in the Java neurons at runtime.

## Neuron network

```
AssetTelemetrySignal (fast loop, unlabeled)
  → CrossSensorReconstructionNeuron   → ReconResidualSignal      (self-supervised)
  → MaintenanceHypothesisNeuron       → HealthHypothesisSignal   (trend/severity/evidence/lead-time)
  → SsAdvisoryGateNeuron              → MaintenanceAdvisorySignal (read-only)

OperatorFeedbackSignal (slow loop)
  → FeedbackAdaptationNeuron          → ThresholdUpdateSignal ─┐
                                                               └→ SsAdvisoryGateNeuron (live, in place)
```

`SsMaintConfig.advisoryOnly` cannot be disabled; the model never writes a device.

## Run

```bash
# 1. Initial (label-free) training -> writes the deployable bundle
python scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py

# 2. Python tests (validate the trainer + label-free separation)
cd scripts/demo-self-supervised-maintenance && python -m unittest tests.test_ss_maintenance

# 3. Java tests (validate the runtime model) — via Maven
mvn -q -pl worker -Dtest=SelfSupervisedMaintenanceModuleTest test
```

`run_all.sh` / `run_all.ps1` run steps 1–2 (Java tests need Maven).

## Deploying against real historical data

Point the trainer at your historian export instead of the synthetic generator
(replace `synth_telemetry.generate` with your canonical rows: the same sensor
names, an operating-regime column, no labels). The trainer fits per-regime
standardisation, cross-sensor weights, and the health-baseline percentiles and
writes them into the layer configuration. Deploy with the Entry runner:

```
java ... Entry local <bundle-jar-url> \
  com.rakovpublic.jneuropallium.worker...IContext \
  worker/src/main/resources/model/self-supervised-maintenance/production-context.json
```

Operators mark advisories confirmed / false-positive; those
`OperatorFeedbackSignal`s flow into `FeedbackAdaptationNeuron`, whose bounded,
rate-limited, domain-shift-frozen updates reshape the gate thresholds in place.

## Honest limits

- Label-free mode detects **degradation**, not a contractually-due repair; fault
  families are heuristic until weak labels accumulate. Expect more false
  positives than a label-trained model — hence the feedback loop.
- Requires a **mostly-healthy** training window (or fleet peers) to define
  normal; a novel asset starts in high-uncertainty / domain-shift mode.
- Stays advisory; the hard safety gate and interlocks are never learned.

No third-party Python packages are required (standard library only).
