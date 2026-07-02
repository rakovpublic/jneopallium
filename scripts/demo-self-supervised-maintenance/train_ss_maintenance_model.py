"""Initial (label-free) training for the Self-Supervised Maintenance model.

This is the ONLY place Python does modelling work. It fits the parameters the
Java neurons load and emits a deployable Jneopallium bundle (layer configs +
descriptor + context). It never sees a fault label: the cross-sensor
reconstruction targets other sensors, and severity is calibrated from the
healthy-window percentiles.

Outputs (worker/src/main/resources/model/self-supervised-maintenance/):
  model-descriptor.json     bundle manifest
  production-context.json    IContext for the Entry runner
  layer-0-input.json ...     per-layer neuron configuration with fitted params
  fitted-model.json          raw fitted parameters (audit / reference)

Run:  python train_ss_maintenance_model.py
"""

from __future__ import annotations

import json
from pathlib import Path

import synth_telemetry as st
from synth_telemetry import SENSORS, REGIMES

SEED = 20240628
TRAIN_TICKS = 2000
TRAIN_ASSETS = [f"PUMP-{100 + i}" for i in range(6)]   # historical fleet, healthy
RIDGE_LAMBDA = 1.0
S = len(SENSORS)

REPO = Path(__file__).resolve().parents[2]
OUT_MODEL = REPO / "worker/src/main/resources/model/self-supervised-maintenance"

NS_N = "com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint"
NS_S = "com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint"
NS_P = "com.rakovpublic.jneuropallium.worker.signalprocessor.impl.ssmaint"


# --------------------------------------------------------------------------- #
# Linear algebra (pure Python ridge regression)
# --------------------------------------------------------------------------- #
def _gauss_solve(a, b):
    n = len(b)
    m = [row[:] + [b[i]] for i, row in enumerate(a)]
    for col in range(n):
        piv = max(range(col, n), key=lambda r: abs(m[r][col]))
        if abs(m[piv][col]) < 1e-12:
            continue
        m[col], m[piv] = m[piv], m[col]
        pv = m[col][col]
        for j in range(col, n + 1):
            m[col][j] /= pv
        for r in range(n):
            if r != col and abs(m[r][col]) > 1e-15:
                f = m[r][col]
                for j in range(col, n + 1):
                    m[r][j] -= f * m[col][j]
    return [m[i][n] for i in range(n)]


def ridge_fit(rows, targets, lam):
    k = len(rows[0]) + 1
    xtx = [[0.0] * k for _ in range(k)]
    xty = [0.0] * k
    for row, y in zip(rows, targets):
        x = row + [1.0]
        for i in range(k):
            xty[i] += x[i] * y
            for j in range(k):
                xtx[i][j] += x[i] * x[j]
    for i in range(k - 1):
        xtx[i][i] += lam
    return _gauss_solve(xtx, xty)


# --------------------------------------------------------------------------- #
# Fit
# --------------------------------------------------------------------------- #
def fit():
    roster = [(a, "healthy") for a in TRAIN_ASSETS]
    data, _ = st.generate(SEED, roster, TRAIN_TICKS, warmup=0, with_faults=False)
    rows = [row for series in data.values() for row in series]

    # per-regime standardisation
    regime_means = [[0.0] * S for _ in range(REGIMES)]
    regime_stds = [[1.0] * S for _ in range(REGIMES)]
    for r in range(REGIMES):
        block = [row for row in rows if row["regime"] == r]
        for i, s in enumerate(SENSORS):
            vals = [row[s] for row in block] or [0.0]
            mean = sum(vals) / len(vals)
            var = sum((v - mean) ** 2 for v in vals) / max(1, len(vals) - 1)
            regime_means[r][i] = mean
            regime_stds[r][i] = var ** 0.5 + 1e-6

    def standardize(row):
        r = row["regime"]
        return [(row[SENSORS[i]] - regime_means[r][i]) / regime_stds[r][i] for i in range(S)]

    z_rows = [standardize(row) for row in rows]

    # cross-sensor reconstruction weights: predict sensor i from the others
    cross = []
    for i in range(S):
        feats = [[z[j] for j in range(S) if j != i] for z in z_rows]
        tgt = [z[i] for z in z_rows]
        cross.append(ridge_fit(feats, tgt, RIDGE_LAMBDA))

    def recon(z):
        tot = 0.0
        for i in range(S):
            others = [z[j] for j in range(S) if j != i]
            pred = cross[i][-1] + sum(cross[i][k] * others[k] for k in range(S - 1))
            tot += (z[i] - pred) ** 2
        return tot / S

    health = sorted(recon(z) for z in z_rows)

    def pct(p):
        return health[min(len(health) - 1, max(0, int(p * (len(health) - 1))))]

    baseline = {
        "mean": sum(health) / len(health),
        "p95": pct(0.95),
        "p99": pct(0.99),
        "p999": pct(0.999),
    }
    return {"regime_means": regime_means, "regime_stds": regime_stds,
            "cross": cross, "baseline": baseline}


# --------------------------------------------------------------------------- #
# Deployable Jneopallium bundle
# --------------------------------------------------------------------------- #
def _neuron(nid, cls, role, results, proc_map, extra=None):
    n = {
        "neuronId": nid,
        "currentNeuronClass": f"{NS_N}.{cls}",
        "resultClasses": [f"{NS_S}.{r}" for r in results],
        "processorMap": {
            f"{NS_S}.{sig}": {
                "signalProcessorClass": f"{NS_P}.{proc}",
                "neuronInterface": f"{NS_N}.{iface}",
            } for sig, (proc, iface) in proc_map.items()
        },
        "mergerMap": {},
        "axon": {"connectionMap": {}, "addressMap": {},
                 "connectionsWrapped": False, "defaultWeights": {}},
        "dendrites": {"weights": {}, "defaultDendritesWeights": {}},
        "signalChain": {
            "clazz": "com.rakovpublic.jneuropallium.ai.neurons.base.SimpleSignalChain",
            "processingChain": [f"{NS_S}.{sig}" for sig in proc_map],
        },
        "isProcessed": False, "changed": False, "onDelete": False, "run": 0,
        "interfaces": [f"{NS_N}.I{cls}"],
        "logicalNeuronRole": role,
        "advisoryOnly": True,
    }
    if extra:
        n.update(extra)
    return n


def write_bundle(model):
    OUT_MODEL.mkdir(parents=True, exist_ok=True)
    families = ["bearing_damage", "cavitation", "sensor_fault", "energy",
                "oscillation", "unknown_anomaly"]
    base_thr = {f: 1.0 for f in families}

    layers = [
        {"layerID": 0, "layerName": "Telemetry Ingest (Unlabeled)",
         "layerType": "input", "labelFree": True,
         "inputSignal": f"{NS_S}.AssetTelemetrySignal",
         "sensors": SENSORS, "neurons": []},
        {"layerID": 1, "layerName": "Self-Supervised Cross-Sensor Reconstruction",
         "layerType": "selfSupervisedHealth", "labelFree": True,
         "neurons": [_neuron(
             0, "CrossSensorReconstructionNeuron", "CrossSensorReconstructionNeuron",
             ["ReconResidualSignal"],
             {"AssetTelemetrySignal": ("CrossSensorReconstructionProcessor",
                                       "ICrossSensorReconstructionNeuron")},
             {"sensorOrder": SENSORS,
              "regimeMeans": model["regime_means"],
              "regimeStds": model["regime_stds"],
              "crossWeights": model["cross"],
              "domainShiftZ": 6.0})]},
        {"layerID": 2, "layerName": "Maintenance Hypothesis (Trend / Severity / Evidence)",
         "layerType": "fusion", "labelFree": True,
         "neurons": [_neuron(
             0, "MaintenanceHypothesisNeuron", "MaintenanceHypothesisNeuron",
             ["HealthHypothesisSignal"],
             {"ReconResidualSignal": ("MaintenanceHypothesisProcessor",
                                      "IMaintenanceHypothesisNeuron")},
             {"baselineMean": model["baseline"]["mean"],
              "baselineP99": model["baseline"]["p99"],
              "baselineP999": model["baseline"]["p999"],
              "slowAlpha": 0.02, "evidenceAlpha": 0.08,
              "phDelta": 0.25, "phLambda": 6.0})]},
        {"layerID": 3, "layerName": "Continuous Feedback Adaptation",
         "layerType": "onlineLearning", "labelFree": True,
         "neurons": [_neuron(
             0, "FeedbackAdaptationNeuron", "FeedbackAdaptationNeuron",
             ["ThresholdUpdateSignal"],
             {"OperatorFeedbackSignal": ("FeedbackAdaptationProcessor",
                                         "IFeedbackAdaptationNeuron")},
             {"baseThresholds": base_thr, "defaultBaseThreshold": 1.0,
              "stepUp": 0.20, "stepDown": 0.01, "maxOffset": 1.6, "minOffset": -0.1,
              "rateLimitTicks": 5, "freezeDomainShift": 0.25,
              "persistTo": "IStorage", "noRedeploy": True})]},
        {"layerID": 4, "layerName": "Advisory Gate (Read-Only)",
         "layerType": "advisoryGate", "labelFree": True,
         "neurons": [_neuron(
             0, "SsAdvisoryGateNeuron", "SsAdvisoryGateNeuron",
             ["MaintenanceAdvisorySignal"],
             {"HealthHypothesisSignal": ("AdvisoryGateProcessor", "ISsAdvisoryGateNeuron"),
              "ThresholdUpdateSignal": ("ThresholdUpdateProcessor", "ISsAdvisoryGateNeuron")},
             {"thresholds": base_thr, "defaultThreshold": 1.0,
              "deduplicationTicks": 60, "neverActuates": True})]},
    ]
    for layer in layers:
        layer["layerSize"] = len(layer["neurons"])
        fname = f"layer-{layer['layerID']}-{layer['layerType'].lower()}.json"
        (OUT_MODEL / fname).write_text(json.dumps(layer, indent=2), encoding="utf-8")

    fitted = {
        "sensors": SENSORS,
        "regimeMeans": model["regime_means"],
        "regimeStds": model["regime_stds"],
        "crossWeights": {SENSORS[i]: [round(w, 6) for w in model["cross"][i]] for i in range(S)},
        "healthBaseline": {k: round(v, 6) for k, v in model["baseline"].items()},
    }
    (OUT_MODEL / "fitted-model.json").write_text(json.dumps(fitted, indent=2), encoding="utf-8")

    signals = ["AssetTelemetrySignal", "ReconResidualSignal", "HealthHypothesisSignal",
               "OperatorFeedbackSignal", "ThresholdUpdateSignal", "MaintenanceAdvisorySignal"]
    descriptor = {
        "modelId": "self-supervised-maintenance",
        "title": "Self-Supervised Maintenance Guardian",
        "version": "1.0.0-label-free-continuous",
        "labelFree": True,
        "continuousLearning": True,
        "redeployRequiredForLearning": False,
        "safetyMode": "ADVISORY",
        "artifact": "fitted-model.json",
        "sensorCount": S,
        "sensors": SENSORS,
        "layers": [{"layerID": l["layerID"], "layerName": l["layerName"],
                    "layerType": l["layerType"], "layerSize": l["layerSize"]} for l in layers],
        "neuronCount": sum(l["layerSize"] for l in layers),
        "signals": [f"{NS_S}.{s}" for s in signals],
        "dataSources": ["UNLABELED_TELEMETRY_HISTORIAN", "OPC_UA_PROCESS_TAGS",
                        "MQTT_IIOT_TELEMETRY", "FLEET_PEER_TELEMETRY",
                        "OPERATOR_FEEDBACK_STREAM"],
        "labelFreeObjectives": ["cross-sensor-reconstruction",
                                "slow-trend-extrapolation", "page-hinkley-change-point",
                                "own-history-severity-calibration"],
        "trainedOn": {"assets": len(TRAIN_ASSETS), "ticksPerAsset": TRAIN_TICKS,
                      "labelsUsed": 0},
        "generatedFrom": {
            "trainer": "scripts/demo-self-supervised-maintenance/train_ss_maintenance_model.py",
            "deterministicSeed": SEED,
            "runtime": "Java neurons under net.neuron.impl.ssmaint (advisory-only)",
        },
    }
    (OUT_MODEL / "model-descriptor.json").write_text(json.dumps(descriptor, indent=2), encoding="utf-8")

    context = {
        "configuration.input.layermeta": [
            {"layerID": l["layerID"], "layerName": l["layerName"], "layerType": l["layerType"]}
            for l in layers],
        "neuronnet.classes": [f"{NS_N}.{c}" for c in [
            "CrossSensorReconstructionNeuron", "MaintenanceHypothesisNeuron",
            "FeedbackAdaptationNeuron", "SsAdvisoryGateNeuron"]],
        "input.inputs": {"type": f"{NS_S}.AssetTelemetrySignal",
                         "source": "OPC_UA/MQTT unlabeled telemetry"},
        "outputAggregator": "com.rakovpublic.jneuropallium.worker.application.impl."
                            "output.JsonlResultAggregator",
        "storage.json": "model/self-supervised-maintenance",
        "processing.frequency.map": {"fast": 1, "slow": 10},
        "slowfast.ratio": 10,
        "isteacherstudying": False,
        "discriminatorsAmount": 0,
        "infiniteRun": True,
        "safetyMode": "ADVISORY",
        "advisoryOnly": True,
        "continuousLearning": True,
    }
    (OUT_MODEL / "production-context.json").write_text(json.dumps(context, indent=2), encoding="utf-8")
    return descriptor


def main():
    print("Self-Supervised Maintenance — initial (label-free) training\n")
    model = fit()
    descriptor = write_bundle(model)
    b = model["baseline"]
    print(f"  Trained on {len(TRAIN_ASSETS)} assets x {TRAIN_TICKS} ticks, labels used: 0")
    print(f"  Sensors                 : {S}")
    print(f"  Health baseline mean    : {b['mean']:.4f}")
    print(f"  Health baseline p99     : {b['p99']:.4f}")
    print(f"  Health baseline p999    : {b['p999']:.4f}")
    print(f"  Neurons in bundle       : {descriptor['neuronCount']}")
    print(f"\n  Deployable bundle -> {OUT_MODEL}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
