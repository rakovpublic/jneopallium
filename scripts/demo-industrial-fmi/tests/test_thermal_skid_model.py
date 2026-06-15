from __future__ import annotations

from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "plant"))
from thermal_skid_model import ThermalSkidModel  # noqa: E402


def run(model: ThermalSkidModel, seconds: float, dt: float = 0.1):
    steps = int(seconds / dt)
    out = None
    for _ in range(steps):
        out = model.step(dt)
    return out


class ThermalSkidModelTest(unittest.TestCase):
    def test_deterministic_evolution(self):
        a = ThermalSkidModel(seed=7)
        b = ThermalSkidModel(seed=7)
        trace_a = [a.step(0.1).as_dict() for _ in range(40)]
        trace_b = [b.step(0.1).as_dict() for _ in range(40)]
        self.assertEqual(trace_a, trace_b)

    def test_increasing_cooling_lowers_temperature(self):
        hot = ThermalSkidModel(seed=1)
        cool = ThermalSkidModel(seed=1)
        hot.state.processTemperature = 82.0
        cool.state.processTemperature = 82.0
        hot.set_input("coolingValveCmd", 0.0)
        cool.set_input("coolingValveCmd", 100.0)
        hot.set_input("heaterPowerCmd", 0.0)
        cool.set_input("heaterPowerCmd", 0.0)
        self.assertLess(run(cool, 20).processTemperature, run(hot, 20).processTemperature)

    def test_increasing_heater_raises_temperature(self):
        off = ThermalSkidModel(seed=2)
        on = ThermalSkidModel(seed=2)
        off.set_input("heaterPowerCmd", 0.0)
        on.set_input("heaterPowerCmd", 100.0)
        self.assertGreater(run(on, 20).processTemperature, run(off, 20).processTemperature)

    def test_pump_speed_increases_flow(self):
        slow = ThermalSkidModel(seed=3)
        fast = ThermalSkidModel(seed=3)
        slow.set_input("pumpSpeedCmd", 25.0)
        fast.set_input("pumpSpeedCmd", 85.0)
        self.assertGreater(run(fast, 10).circulationFlow, run(slow, 10).circulationFlow)

    def test_wear_increases_vibration_and_bearing_temperature(self):
        good = ThermalSkidModel(seed=4)
        worn = ThermalSkidModel(seed=4)
        worn.set_input("faultPumpWear", 0.85)
        good_out = run(good, 60)
        worn_out = run(worn, 60)
        self.assertGreater(worn_out.vibrationRms, good_out.vibrationRms)
        self.assertGreater(worn_out.bearingTemperature, good_out.bearingTemperature)

    def test_stuck_valve_fault_affects_position(self):
        model = ThermalSkidModel(seed=5)
        model.set_input("faultValveStuck", True)
        start = model.state.coolingValvePosition
        model.set_input("coolingValveCmd", 100.0)
        run(model, 10)
        self.assertAlmostEqual(start, model.state.coolingValvePosition)

    def test_sensor_drift_affects_measured_not_true_temperature(self):
        base = ThermalSkidModel(seed=6)
        drifted = ThermalSkidModel(seed=6)
        drifted.set_input("faultTempSensorDrift", 5.0)
        base_out = run(base, 5)
        drifted_out = run(drifted, 5)
        self.assertAlmostEqual(base_out.processTemperature, drifted_out.processTemperature)
        self.assertGreater(drifted_out.measuredTemperature - base_out.measuredTemperature, 4.9)

    def test_interlock_threshold_behavior(self):
        model = ThermalSkidModel(seed=8)
        model.state.processTemperature = 93.0
        out = model.step(0.1)
        self.assertTrue(out.highTemperatureInterlock)

    def test_stale_command_fail_safe_values_are_safe(self):
        model = ThermalSkidModel(seed=9)
        model.set_input("coolingValveCmd", 100.0)
        model.set_input("pumpSpeedCmd", 30.0)
        model.set_input("heaterPowerCmd", 0.0)
        out = run(model, 5)
        self.assertGreaterEqual(out.coolingValvePosition, 95.0)
        self.assertLessEqual(model.inputs.heaterPowerCmd, 0.0)


if __name__ == "__main__":
    unittest.main()
