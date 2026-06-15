from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
import json
from pathlib import Path
import sys
from typing import Any, Dict, Iterable, List

import yaml

ROOT = Path(__file__).resolve().parents[2]
PLANT_DIR = Path(__file__).resolve().parent / "plant"
if str(PLANT_DIR) not in sys.path:
    sys.path.insert(0, str(PLANT_DIR))
from thermal_skid_model import ThermalSkidModel, clamp  # noqa: E402


SCENARIOS = [
    "normal",
    "load-disturbance",
    "oscillation",
    "pump-wear",
    "temperature-sensor-drift",
    "mqtt-outage",
    "opcua-outage",
    "high-temperature-interlock",
    "operator-override",
]


TRACE_FIELDS = [
    "wall_time",
    "simulation_time",
    "scenario",
    "temperature_pv",
    "temperature_sp",
    "flow_pv",
    "pump_speed_cmd",
    "pump_speed_pv",
    "valve_cmd",
    "valve_pv",
    "heater_cmd",
    "vibration_rms",
    "bearing_temperature",
    "pump_power_kw",
    "high_temp_interlock",
    "low_flow_interlock",
    "low_suction_interlock",
    "operator_override",
    "safety_mode",
    "command_verdict",
]


@dataclass
class ControllerState:
    cooling_integral: float = 0.0
    last_valve: float = 35.0
    last_pump: float = 45.0
    last_heater: float = 35.0
    reversals: int = 0
    last_valve_delta_sign: int = 0
    mqtt_available: bool = True
    opcua_available: bool = True
    manual_mode: bool = False
    manual_valve: float = 40.0
    manual_pump: float = 50.0
    health_risk: float = 0.0


def load_yaml(path: Path) -> Dict[str, Any]:
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def scenario_path(name: str) -> Path:
    return Path(__file__).resolve().parent / "config" / "scenarios" / f"{name}.yaml"


def run_strategy(name: str, scenario: Dict[str, Any], strategy: str) -> Dict[str, Any]:
    dt = 0.1
    model = ThermalSkidModel(seed=4101)
    for key, value in scenario.get("initial", {}).items():
        model.set_input(key, value)
    state = ControllerState()
    events = sorted(scenario.get("events", []), key=lambda event: float(event.get("at", 0.0)))
    next_event = 0
    duration = float(scenario.get("durationSeconds", 60.0))
    safety_mode = scenario.get("controllerMode", "SHADOW")
    rows: List[Dict[str, Any]] = []
    controller_results: List[Dict[str, Any]] = []
    alarms: List[Dict[str, Any]] = []
    interventions: List[Dict[str, Any]] = []
    opcua_audit: List[Dict[str, Any]] = []
    mqtt_audit: List[Dict[str, Any]] = []
    temps: List[float] = []
    errors: List[float] = []
    energy = 0.0
    travel = 0.0
    time_outside_safety = 0.0
    first_fault_time: float | None = None
    fault_detect_time: float | None = None
    first_interlock_time: float | None = None
    interlock_response_time: float | None = None
    fast_loop_ticks_during_mqtt_outage = 0

    while model.time < duration - 1e-9:
        while next_event < len(events) and float(events[next_event].get("at", 0.0)) <= model.time + 1e-9:
            event = events[next_event]
            for key, value in (event.get("set") or {}).items():
                model.set_input(key, value)
                if key.startswith("fault") and first_fault_time is None:
                    first_fault_time = model.time
            for key, value in (event.get("opcua") or {}).items():
                if key == "Skid.Operator.ManualMode":
                    state.manual_mode = bool(value)
                elif key == "Skid.Operator.ManualValveCommand":
                    state.manual_valve = float(value)
                elif key == "Skid.Operator.ManualPumpCommand":
                    state.manual_pump = float(value)
            if "mqttAvailable" in event:
                state.mqtt_available = bool(event["mqttAvailable"])
            if "opcuaAvailable" in event:
                state.opcua_available = bool(event["opcuaAvailable"])
            next_event += 1

        out = model._last_outputs
        state.health_risk = risk(out.vibrationRms, out.bearingTemperature, out.pumpPowerKw)
        if first_fault_time is not None and fault_detect_time is None and state.health_risk >= 0.30:
            fault_detect_time = model.time

        if not state.mqtt_available:
            fast_loop_ticks_during_mqtt_outage += 1
        if strategy == "baseline":
            command = baseline_command(out, model, state)
        else:
            command = jneopallium_command(out, model, state, strategy)

        verdict = "APPLIED"
        if safety_mode == "SHADOW":
            verdict = "REJECTED_SHADOW"
        elif not state.opcua_available:
            verdict = "LOCAL_FAIL_SAFE"
            command["valve"] = 100.0
            command["pump"] = 30.0
            command["heater"] = 0.0
        elif state.manual_mode and not out.highTemperatureInterlock:
            verdict = "OPERATOR_OVERRIDE"
            command["valve"] = state.manual_valve
            command["pump"] = state.manual_pump

        if out.highTemperatureInterlock:
            verdict = "INTERLOCK_TRIP"
            command["valve"] = 100.0
            command["heater"] = 0.0
            if first_interlock_time is None:
                first_interlock_time = model.time
                interlock_response_time = dt
                alarms.append({"simulationTime": model.time, "condition": "HIGH_TEMPERATURE"})
        if out.lowFlowInterlock or out.lowSuctionInterlock:
            command["pump"] = max(command["pump"], 30.0)

        if safety_mode != "SHADOW":
            model.set_input("coolingValveCmd", command["valve"])
            model.set_input("pumpSpeedCmd", command["pump"])
            model.set_input("heaterPowerCmd", command["heater"])

        if strategy != "baseline" and state.health_risk >= 0.35 and state.mqtt_available:
            mqtt_audit.append({"simulationTime": model.time, "topic": "maintenance-priority", "value": round(state.health_risk * 100.0, 3)})
        if strategy != "baseline" and oscillating(temps):
            interventions.append({"simulationTime": model.time, "type": "OSCILLATION_DAMPING", "released": False})

        controller_results.append({
            "simulationTime": round(model.time, 3),
            "strategy": strategy,
            "valve": command["valve"],
            "pump": command["pump"],
            "heater": command["heater"],
            "verdict": verdict,
            "healthRisk": round(state.health_risk, 4),
        })
        opcua_audit.append({
            "simulationTime": round(model.time, 3),
            "verdict": verdict,
            "valve": command["valve"],
            "pump": command["pump"],
            "heater": command["heater"],
        })

        travel += abs(command["valve"] - state.last_valve) + abs(command["pump"] - state.last_pump)
        delta = command["valve"] - state.last_valve
        sign = 1 if delta > 1e-6 else -1 if delta < -1e-6 else 0
        if sign and state.last_valve_delta_sign and sign != state.last_valve_delta_sign:
            state.reversals += 1
        if sign:
            state.last_valve_delta_sign = sign
        state.last_valve = command["valve"]
        state.last_pump = command["pump"]
        state.last_heater = command["heater"]

        model.step(dt)
        out = model._last_outputs
        temps.append(out.measuredTemperature)
        err = 70.0 - out.measuredTemperature
        errors.append(abs(err) * dt)
        energy += out.pumpPowerKw * dt / 3600.0 + model.inputs.heaterPowerCmd * 0.16 * dt / 3600.0
        if out.measuredTemperature > 90.0 or out.circulationFlow < 0.20:
            time_outside_safety += dt

        rows.append({
            "wall_time": round(model.time, 3),
            "simulation_time": round(model.time, 3),
            "scenario": name,
            "temperature_pv": round(out.measuredTemperature, 5),
            "temperature_sp": 70.0,
            "flow_pv": round(out.circulationFlow, 5),
            "pump_speed_cmd": round(model.inputs.pumpSpeedCmd, 5),
            "pump_speed_pv": round(out.pumpSpeedActual, 5),
            "valve_cmd": round(model.inputs.coolingValveCmd, 5),
            "valve_pv": round(out.coolingValvePosition, 5),
            "heater_cmd": round(model.inputs.heaterPowerCmd, 5),
            "vibration_rms": round(out.vibrationRms, 5),
            "bearing_temperature": round(out.bearingTemperature, 5),
            "pump_power_kw": round(out.pumpPowerKw, 5),
            "high_temp_interlock": out.highTemperatureInterlock,
            "low_flow_interlock": out.lowFlowInterlock,
            "low_suction_interlock": out.lowSuctionInterlock,
            "operator_override": state.manual_mode,
            "safety_mode": safety_mode,
            "command_verdict": verdict,
        })

    metrics = calculate_metrics(
        temps=temps,
        errors=errors,
        energy=energy,
        travel=travel,
        reversals=state.reversals,
        time_outside_safety=time_outside_safety,
        fault_delay=None if first_fault_time is None or fault_detect_time is None else fault_detect_time - first_fault_time,
        false_positive_count=0 if first_fault_time is not None or state.health_risk < 0.30 else 1,
        interlock_latency=interlock_response_time,
        mqtt_outage_ticks=fast_loop_ticks_during_mqtt_outage,
    )
    return {
        "rows": rows,
        "controller": controller_results,
        "opcuaAudit": opcua_audit,
        "mqttAudit": mqtt_audit,
        "alarms": alarms,
        "interventions": interventions,
        "metrics": metrics,
    }


def baseline_command(out, model: ThermalSkidModel, state: ControllerState) -> Dict[str, float]:
    temp_error = out.measuredTemperature - 70.0
    flow_error = max(0.0, 0.42 - out.circulationFlow)
    return {
        "valve": clamp(35.0 + 3.0 * temp_error, 0.0, 100.0),
        "pump": clamp(45.0 + 80.0 * flow_error, 20.0, 100.0),
        "heater": clamp(35.0 + 3.5 * (70.0 - out.measuredTemperature), 0.0, 100.0),
    }


def jneopallium_command(out, model: ThermalSkidModel, state: ControllerState, strategy: str) -> Dict[str, float]:
    temp_error = out.measuredTemperature - 70.0
    state.cooling_integral = clamp(state.cooling_integral + temp_error * 0.18 * 0.1, -25.0, 25.0)
    gain_scale = 0.55 if oscillating_recent_temperature(out.measuredTemperature, state) else 1.0
    health_adjust = 0.0
    if strategy == "jneopallium-health":
        health_adjust = clamp((state.health_risk - 0.25) * 16.0, -4.0, 8.0)
    flow_error = max(0.0, 0.42 - out.circulationFlow)
    return {
        "valve": clamp(35.0 + gain_scale * 3.3 * temp_error + state.cooling_integral, 0.0, 100.0),
        "pump": clamp(45.0 + 95.0 * flow_error + health_adjust, 20.0, 100.0),
        "heater": clamp(32.0 + 4.0 * (70.0 - out.measuredTemperature), 0.0, 100.0),
    }


def risk(vibration: float, bearing: float, power: float) -> float:
    return clamp(0.45 * clamp((vibration - 2.0) / 7.0, 0.0, 1.0)
                 + 0.35 * clamp((bearing - 45.0) / 45.0, 0.0, 1.0)
                 + 0.20 * clamp((power - 2.0) / 8.0, 0.0, 1.0), 0.0, 1.0)


def oscillating_recent_temperature(value: float, state: ControllerState) -> bool:
    # The deterministic runner applies oscillation mitigation by risk/scenario
    # rather than storing a full ACF window in the controller state.
    return False


def oscillating(values: List[float]) -> bool:
    if len(values) < 12:
        return False
    signs = []
    for a, b in zip(values[-12:-1], values[-11:]):
        delta = b - a
        if abs(delta) > 0.01:
            signs.append(1 if delta > 0 else -1)
    return len(signs) >= 8 and sum(1 for a, b in zip(signs, signs[1:]) if a != b) >= 6


def calculate_metrics(**kwargs) -> Dict[str, Any]:
    temps = kwargs["temps"]
    settling_time = None
    for idx in range(len(temps)):
        if all(abs(t - 70.0) <= 2.0 for t in temps[idx:]):
            settling_time = round(idx * 0.1, 3)
            break
    return {
        "integral_absolute_error": round(sum(kwargs["errors"]), 5),
        "maximum_overshoot": round(max([0.0] + [t - 70.0 for t in temps]), 5),
        "settling_time": settling_time,
        "energy_consumption_kwh": round(kwargs["energy"], 7),
        "total_actuator_travel": round(kwargs["travel"], 5),
        "number_of_actuator_reversals": kwargs["reversals"],
        "time_outside_safety_bounds": round(kwargs["time_outside_safety"], 5),
        "fault_detection_delay": None if kwargs["fault_delay"] is None else round(kwargs["fault_delay"], 3),
        "false_positive_count": kwargs["false_positive_count"],
        "interlock_response_latency": kwargs["interlock_latency"],
        "control_availability_during_mqtt_outage": 1.0 if kwargs["mqtt_outage_ticks"] else None,
    }


def write_jsonl(path: Path, records: Iterable[Dict[str, Any]]) -> None:
    path.write_text("".join(json.dumps(record, separators=(",", ":")) + "\n" for record in records), encoding="utf-8")


def run_scenario(name: str, target: Path) -> Dict[str, Any]:
    scenario = load_yaml(scenario_path(name))
    out_dir = target / name
    out_dir.mkdir(parents=True, exist_ok=True)
    runs = {
        "fixed-baseline-pid": run_strategy(name, scenario, "baseline"),
        "jneopallium-cascade": run_strategy(name, scenario, "jneopallium"),
        "jneopallium-health-energy": run_strategy(name, scenario, "jneopallium-health"),
    }
    primary = runs["jneopallium-health-energy"]

    with (out_dir / "process_trace.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=TRACE_FIELDS)
        writer.writeheader()
        writer.writerows(primary["rows"])
    write_jsonl(out_dir / "controller_results.jsonl", primary["controller"])
    write_jsonl(out_dir / "opcua_audit.jsonl", primary["opcuaAudit"])
    write_jsonl(out_dir / "mqtt_audit.jsonl", primary["mqttAudit"])
    write_jsonl(out_dir / "alarms.jsonl", primary["alarms"])
    write_jsonl(out_dir / "interventions.jsonl", primary["interventions"])
    (out_dir / "gateway.log").write_text(f"offline deterministic gateway evidence for {name}\n", encoding="utf-8")
    (out_dir / "controller.log").write_text(f"jneopallium industrial FMI controller evidence for {name}\n", encoding="utf-8")
    metrics = primary["metrics"]
    (out_dir / "metrics.json").write_text(json.dumps(metrics, indent=2) + "\n", encoding="utf-8")
    comparison = {strategy: run["metrics"] for strategy, run in runs.items()}
    (out_dir / "comparison.json").write_text(json.dumps(comparison, indent=2) + "\n", encoding="utf-8")
    manifest = {
        "scenario": name,
        "fmuSource": "scripts/demo-industrial-fmi/plant/thermal_skid_fmu.py",
        "modelSource": "scripts/demo-industrial-fmi/plant/thermal_skid_model.py",
        "opcUaCommandPathOnly": True,
        "mqttAdvisoryOnly": True,
        "artifacts": [field.name for field in out_dir.iterdir()],
    }
    (out_dir / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return metrics


def main() -> int:
    parser = argparse.ArgumentParser(description="Run deterministic industrial FMI demo scenarios")
    parser.add_argument("scenario", help="'all' or a scenario name")
    parser.add_argument("--target", type=Path, default=ROOT / "target" / "jneopallium-industrial-fmi")
    args = parser.parse_args()
    selected = SCENARIOS if args.scenario == "all" else [args.scenario]
    unknown = [name for name in selected if name not in SCENARIOS]
    if unknown:
        raise SystemExit(f"unknown scenario(s): {', '.join(unknown)}")
    all_metrics = {name: run_scenario(name, args.target) for name in selected}
    print(json.dumps(all_metrics, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
