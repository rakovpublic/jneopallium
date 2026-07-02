"""Tests for the initial (label-free) training step.

These verify the *trainer* — the Python responsibility. The runtime detection
and continuous-learning behaviour are tested in Java
(SelfSupervisedMaintenanceModuleTest). Here we confirm the fitted parameters are
sane and that, applied exactly as the Java CrossSensorReconstructionNeuron
applies them, they separate injected degradations from healthy operation — all
without any label having been used to fit them.
"""

import json
import sys
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT))

import synth_telemetry as st                      # noqa: E402
from synth_telemetry import SENSORS               # noqa: E402
import train_ss_maintenance_model as trainer      # noqa: E402

S = len(SENSORS)


def standardize(model, row):
    r = row["regime"]
    return [(row[SENSORS[i]] - model["regime_means"][r][i]) / model["regime_stds"][r][i]
            for i in range(S)]


def recon(model, z):
    """Replicates CrossSensorReconstructionNeuron.reconstruct (total + residuals)."""
    cross = model["cross"]
    residuals = {}
    total = 0.0
    for i in range(S):
        others = [z[j] for j in range(S) if j != i]
        pred = cross[i][-1] + sum(cross[i][k] * others[k] for k in range(S - 1))
        res = z[i] - pred
        residuals[SENSORS[i]] = res
        total += res * res
    return total / S, residuals


class TrainingTest(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.model = trainer.fit()

    def test_baseline_percentiles_ordered(self):
        b = self.model["baseline"]
        self.assertLess(b["mean"], b["p95"])
        self.assertLess(b["p95"], b["p99"])
        self.assertLess(b["p99"], b["p999"])

    def test_cross_weights_finite(self):
        for row in self.model["cross"]:
            self.assertEqual(len(row), S)      # (S-1) predictors + bias
            for w in row:
                self.assertTrue(abs(w) < 1e6 and w == w)

    def test_healthy_frames_low_residual(self):
        data, _ = st.generate(7, [("H1", "healthy")], 1500, warmup=0, with_faults=False)
        p99 = self.model["baseline"]["p99"]
        highs = 0
        for row in data["H1"][500:]:
            total, _ = recon(self.model, standardize(self.model, row))
            if total > p99:
                highs += 1
        # a healthy asset should rarely exceed its own p99 band
        self.assertLess(highs / 1000.0, 0.05)

    def test_faults_separate_without_labels(self):
        roster = [("B", "bearing_wear"), ("C", "cavitation"), ("D", "sensor_drift"),
                  ("E", "energy_drift"), ("O", "oscillation")]
        data, oracle = st.generate(9, roster, 2200, warmup=300, with_faults=True)
        p999 = self.model["baseline"]["p999"]
        dominant = {
            "bearing_wear": {"vibration_rms", "bearing_temp"},
            "cavitation": {"suction_pressure", "vibration_rms"},
            "sensor_drift": {"process_temp"},
            "energy_drift": {"pump_power"},
            "oscillation": {"valve_position", "flow"},
        }
        for asset, o in oracle.items():
            # detection is windowed; take the peak over the last ticks of the ramp
            window = range(max(o["onset"], o["event"] - 30), o["event"] + 1)
            best_total, best_res = 0.0, None
            for t in window:
                total, residuals = recon(self.model, standardize(self.model, data[asset][t]))
                if total > best_total:
                    best_total, best_res = total, residuals
            self.assertGreater(best_total, 2 * p999,
                               f"{o['family']} not separated from healthy baseline")
            top = max(best_res, key=lambda k: abs(best_res[k]))
            self.assertIn(top, dominant[o["family"]],
                          f"{o['family']} dominant sensor was {top}")

    def test_bundle_written_and_valid(self):
        trainer.write_bundle(self.model)
        out = trainer.OUT_MODEL
        descriptor = json.loads((out / "model-descriptor.json").read_text(encoding="utf-8"))
        self.assertTrue(descriptor["labelFree"])
        self.assertTrue(descriptor["continuousLearning"])
        self.assertFalse(descriptor["redeployRequiredForLearning"])
        self.assertEqual(descriptor["trainedOn"]["labelsUsed"], 0)
        self.assertEqual(descriptor["safetyMode"], "ADVISORY")
        for name in ["fitted-model.json", "production-context.json",
                     "layer-1-selfsupervisedhealth.json", "layer-4-advisorygate.json"]:
            self.assertTrue((out / name).exists(), f"missing {name}")
        # the reconstruction layer must carry the fitted weights
        layer1 = json.loads((out / "layer-1-selfsupervisedhealth.json").read_text(encoding="utf-8"))
        neuron = layer1["neurons"][0]
        self.assertEqual(len(neuron["crossWeights"]), S)
        self.assertEqual(neuron["sensorOrder"], SENSORS)


if __name__ == "__main__":
    unittest.main(verbosity=2)
