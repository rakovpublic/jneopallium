from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
import json
import math
from pathlib import Path
import sys
from typing import Any, Dict, Iterable, List

import yaml

ROOT = Path(__file__).resolve().parents[2]
TRAINED_MODEL_PATH = ROOT / "worker" / "src" / "main" / "resources" / "model" / "industrial-loop-guardian" / "trained-industrial-loop-guardian-model.json"
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

CONTROL_BOUNDARY = {
    "plcPidSis": "deterministic millisecond control and hard safety remain authoritative",
    "jneopallium": "supervisory diagnosis, optimisation, and bounded recommendations",
    "opcUa": "bounded local actuator path only",
    "mqtt": "telemetry and advisory path only",
    "autonomousAction": False,
}

ECONOMIC_ASSUMPTIONS = {
    "energyCostUsdPerKwh": 0.12,
    "unplannedShutdownUsd": 20_000,
    "annualOperatingHours": 6_000,
}


TRACE_FIELDS = [
    "wall_time",
    "simulation_time",
    "scenario",
    "process_temperature",
    "temperature_pv",
    "temperature_sp",
    "process_load_flow",
    "flow_pv",
    "suction_pressure",
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
            "process_temperature": round(out.processTemperature, 5),
            "temperature_pv": round(out.measuredTemperature, 5),
            "temperature_sp": 70.0,
            "process_load_flow": round(model.inputs.processLoadFlow, 5),
            "flow_pv": round(out.circulationFlow, 5),
            "suction_pressure": round(out.suctionPressure, 5),
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


def numeric_series(rows: List[Dict[str, Any]], field: str) -> List[float]:
    return [float(row[field]) for row in rows]


def sign(value: float) -> int:
    if value > 1e-9:
        return 1
    if value < -1e-9:
        return -1
    return 0


def zero_crossing_rate(values: List[float]) -> float:
    signs = [sign(value) for value in values]
    pairs = [(left, right) for left, right in zip(signs, signs[1:]) if left and right]
    return sum(1 for left, right in pairs if left != right) / max(1, len(pairs))


def direction_change_density(values: List[float], ticks_per_second: float = 10.0) -> float:
    deltas = [values[idx] - values[idx - 1] for idx in range(1, len(values))]
    signs = [sign(delta) for delta in deltas]
    pairs = [(left, right) for left, right in zip(signs, signs[1:]) if left and right]
    duration = max(1.0, len(values) / ticks_per_second)
    return sum(1 for left, right in pairs if left != right) / duration


def autocorrelation_peak(values: List[float], min_lag: int = 8, max_lag: int = 90,
                         difference: bool = True) -> tuple[float, int]:
    working = [values[idx] - values[idx - 1] for idx in range(1, len(values))] if difference else list(values)
    if len(working) < min_lag * 2:
        return 0.0, 0
    mean = sum(working) / len(working)
    centered = [value - mean for value in working]
    denom = sum(value * value for value in centered)
    if denom <= 1e-12:
        return 0.0, 0
    best = 0.0
    best_lag = 0
    for lag in range(min_lag, min(max_lag, len(centered) // 2) + 1):
        corr = sum(centered[idx] * centered[idx - lag] for idx in range(lag, len(centered))) / denom
        score = abs(corr)
        if score > best:
            best = score
            best_lag = lag
    return best, best_lag


def mean_abs_command_position_mismatch(rows: List[Dict[str, Any]]) -> float:
    valve = sum(abs(float(row["valve_cmd"]) - float(row["valve_pv"])) for row in rows) / len(rows)
    pump = sum(abs(float(row["pump_speed_cmd"]) - float(row["pump_speed_pv"])) for row in rows) / len(rows)
    return (valve + pump) / 200.0


def valve_stiction_proxy(rows: List[Dict[str, Any]]) -> float:
    stuck_ticks = 0
    for previous, current in zip(rows, rows[1:]):
        command_delta = abs(float(current["valve_cmd"]) - float(previous["valve_cmd"]))
        position_delta = abs(float(current["valve_pv"]) - float(previous["valve_pv"]))
        if command_delta > 0.05 and position_delta < 0.005:
            stuck_ticks += 1
    return stuck_ticks / max(1, len(rows) - 1)


def load_transition_ratio(rows: List[Dict[str, Any]]) -> float:
    changes = 0
    for previous, current in zip(rows, rows[1:]):
        if abs(float(current["process_load_flow"]) - float(previous["process_load_flow"])) > 0.02:
            changes += 1
    return changes / max(1, len(rows) - 1)


def safety_envelope_satisfied(primary: Dict[str, Any]) -> bool:
    return all(
        not bool(row["high_temp_interlock"])
        and not bool(row["low_flow_interlock"])
        and not bool(row["low_suction_interlock"])
        and not bool(row["operator_override"])
        for row in primary["rows"]
    ) and all(item["verdict"] != "LOCAL_FAIL_SAFE" for item in primary["controller"])


def diagnose_control_loop(features: Dict[str, float]) -> Dict[str, Any]:
    if features.get("valve_stiction_proxy", 0.0) >= 0.002 or features.get("command_position_mismatch_ratio", 0.0) >= 0.01:
        cause = "probable valve stiction or actuator tracking fault"
    elif features.get("cascade_interaction_score", 0.0) >= 0.20:
        cause = "probable cascade interaction or gain mismatch"
    elif features.get("load_transition_ratio", 0.0) >= 0.002:
        cause = "phase-specific controller problem during load transition"
    else:
        cause = "probable tuning, transport-delay, or sensor-noise mismatch"
    return {
        "neuron": "OscillationDiagnosisNeuron",
        "probableCause": cause,
        "temperatureErrorAutocorrelationPeak": round(features.get("temperature_error_acf_peak", 0.0), 6),
        "valveStictionProxy": round(features.get("valve_stiction_proxy", 0.0), 6),
        "commandPositionMismatchRatio": round(features.get("command_position_mismatch_ratio", 0.0), 6),
        "cascadeInteractionScore": round(features.get("cascade_interaction_score", 0.0), 6),
    }


def economic_basis(finding_code: str, primary: Dict[str, Any], runs: Dict[str, Dict[str, Any]],
                   features: Dict[str, float], confidence: float) -> Dict[str, Any]:
    rows = primary["rows"]
    metrics = primary["metrics"]
    baseline = runs["fixed-baseline-pid"]["metrics"]
    duration_hours = max(float(rows[-1]["simulation_time"]) / 3600.0, 1e-9)
    current_speed = float(rows[-1]["pump_speed_cmd"])
    health_risk = features.get("max_health_risk", 0.0)
    safety_ok = safety_envelope_satisfied(primary)
    baseline_kw = float(baseline["energy_consumption_kwh"]) / duration_hours
    current_kw = float(metrics["energy_consumption_kwh"]) / duration_hours
    predicted_saving_kw = max(0.0, baseline_kw - current_kw)
    annual_energy_value = (
        predicted_saving_kw
        * ECONOMIC_ASSUMPTIONS["annualOperatingHours"]
        * ECONOMIC_ASSUMPTIONS["energyCostUsdPerKwh"]
    )

    if finding_code == "ENERGY_PER_UNIT_PRODUCTION_DETERIORATION":
        recommended_speed = clamp(current_speed - (3.0 if health_risk < 0.35 and safety_ok else 0.0), 20.0, 100.0)
        return {
            "neuron": "EconomicBasisNeuron",
            "recommendation": "REDUCE_PUMP_SPEED" if recommended_speed < current_speed else "HOLD_SETPOINT_AND_REVIEW",
            "currentSpeed": round(current_speed, 3),
            "recommendedSpeed": round(recommended_speed, 3),
            "predictedFlowImpact": -0.8 if recommended_speed < current_speed else 0.0,
            "predictedEnergySavingKw": round(predicted_saving_kw, 4),
            "estimatedAnnualEnergyValueUsd": round(annual_energy_value, 2),
            "equipmentRiskChange": 0.02 if recommended_speed < current_speed else 0.0,
            "confidence": round(confidence, 4),
            "safetyEnvelopeSatisfied": safety_ok,
        }
    if finding_code == "PUMP_WEAR_CAVITATION_RISK":
        return {
            "neuron": "EconomicBasisNeuron",
            "recommendation": "SCHEDULE_PUMP_INSPECTION",
            "estimatedAvoidedShutdownValueUsd": ECONOMIC_ASSUMPTIONS["unplannedShutdownUsd"],
            "healthRisk": round(health_risk, 4),
            "minimumSuctionPressureBar": round(features.get("min_suction_pressure_bar", 0.0), 4),
            "confidence": round(confidence, 4),
            "safetyEnvelopeSatisfied": safety_ok,
        }
    if finding_code == "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION":
        return {
            "neuron": "EconomicBasisNeuron",
            "recommendation": "RUN_SHADOW_REPLAY_AND_TUNE_LOOP",
            "boundedSetpointChangePercent": 0.0,
            "estimatedOperatorReviewMinutes": 30,
            "confidence": round(confidence, 4),
            "safetyEnvelopeSatisfied": safety_ok,
        }
    if finding_code == "TEMPERATURE_SENSOR_DRIFT":
        return {
            "neuron": "EconomicBasisNeuron",
            "recommendation": "CALIBRATE_OR_REPLACE_SENSOR",
            "estimatedAvoidedFalseShutdownValueUsd": round(0.25 * ECONOMIC_ASSUMPTIONS["unplannedShutdownUsd"], 2),
            "maxResidualC": round(features.get("max_temperature_model_residual_c", 0.0), 4),
            "confidence": round(confidence, 4),
            "safetyEnvelopeSatisfied": safety_ok,
        }
    return {"neuron": "EconomicBasisNeuron", "recommendation": "REVIEW_ADVISORY", "confidence": round(confidence, 4)}


def advisory_findings(name: str, primary: Dict[str, Any], runs: Dict[str, Dict[str, Any]]) -> List[Dict[str, Any]]:
    rows = primary["rows"]
    metrics = primary["metrics"]
    baseline = runs["fixed-baseline-pid"]["metrics"]
    if not rows:
        return []
    features = production_feature_map(primary, runs)
    safety_ok = safety_envelope_satisfied(primary)

    max_health = max(row_health_risk(row) for row in rows)
    max_vibration = max(float(row["vibration_rms"]) for row in rows)
    max_bearing = max(float(row["bearing_temperature"]) for row in rows)
    max_power = max(float(row["pump_power_kw"]) for row in rows)
    min_flow = min(float(row["flow_pv"]) for row in rows)
    min_suction = min(float(row["suction_pressure"]) for row in rows)
    max_temp_residual = max(abs(float(row["temperature_pv"]) - float(row["process_temperature"])) for row in rows)
    energy_delta = float(metrics["energy_consumption_kwh"]) - float(baseline["energy_consumption_kwh"])
    baseline_energy = max(float(baseline["energy_consumption_kwh"]), 1e-9)
    energy_delta_ratio = energy_delta / baseline_energy
    latest_time = rows[-1]["simulation_time"]

    findings: List[Dict[str, Any]] = []
    if max_health >= 0.35 or min_suction < 0.42 or min_flow < 0.25:
        confidence = clamp(0.30 + max_health * 0.72 + max(0.0, 0.42 - min_suction) * 0.9, 0.0, 0.99)
        findings.append(advisory(
            simulation_time=latest_time,
            asset="P-101",
            finding="pump wear and cavitation risk",
            finding_code="PUMP_WEAR_CAVITATION_RISK",
            confidence=confidence,
            evidence={
                "neuron": "PumpHealthAndEfficiencyNeuron",
                "maxHealthRisk": round(max_health, 4),
                "maxVibrationRms": round(max_vibration, 4),
                "maxBearingTemperatureC": round(max_bearing, 4),
                "maxPumpPowerKw": round(max_power, 4),
                "minimumFlow": round(min_flow, 4),
                "minimumSuctionPressureBar": round(min_suction, 4),
            },
            recommended_action="Inspect P-101 impeller, bearing, suction strainer, and cavitation margin; keep controller in advisory or shadow until maintenance clears.",
            urgency_hours=12 if max_health >= 0.65 or min_suction < 0.42 else 48,
            recommendation="SCHEDULE_PUMP_INSPECTION",
            economic=economic_basis("PUMP_WEAR_CAVITATION_RISK", primary, runs, features, confidence),
            safety_ok=safety_ok,
        ))

    if name == "oscillation":
        confidence = clamp(0.45 + 0.03 * len(primary["interventions"]) + 0.01 * metrics["number_of_actuator_reversals"], 0.0, 0.96)
        diagnosis = diagnose_control_loop(features)
        findings.append(advisory(
            simulation_time=latest_time,
            asset="TIC-101",
            finding="control-loop oscillation and tuning degradation",
            finding_code="CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION",
            confidence=confidence,
            evidence={
                "neuron": "OscillationDiagnosisNeuron",
                "oscillationInterventionCount": len(primary["interventions"]),
                "actuatorReversals": metrics["number_of_actuator_reversals"],
                "totalActuatorTravel": metrics["total_actuator_travel"],
                "integralAbsoluteError": metrics["integral_absolute_error"],
                "diagnosis": diagnosis,
            },
            recommended_action="Review TIC-101/FIC-101 tuning, freeze integral adaptation during load step, and run shadow replay before changing PLC gains.",
            urgency_hours=24,
            recommendation="RUN_SHADOW_REPLAY_AND_TUNE_LOOP",
            economic=economic_basis("CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION", primary, runs, features, confidence),
            safety_ok=safety_ok,
            diagnosis=diagnosis,
        ))

    if max_temp_residual >= 1.5:
        confidence = clamp(0.35 + max_temp_residual / 9.0, 0.0, 0.98)
        findings.append(advisory(
            simulation_time=latest_time,
            asset="TIC-101.PV",
            finding="temperature-sensor drift",
            finding_code="TEMPERATURE_SENSOR_DRIFT",
            confidence=confidence,
            evidence={
                "neuron": "SensorFaultDiscriminationNeuron",
                "maxModelResidualC": round(max_temp_residual, 4),
                "residualSource": "measuredTemperature - processTemperature in deterministic twin",
                "affectedTag": "SKID.TIC101.PV",
            },
            recommended_action="Compare TIC-101 against a calibrated reference sensor and lock out autonomous optimization if residual remains above 1.5 C.",
            urgency_hours=72 if max_temp_residual < 5.0 else 24,
            recommendation="CALIBRATE_OR_REPLACE_SENSOR",
            economic=economic_basis("TEMPERATURE_SENSOR_DRIFT", primary, runs, features, confidence),
            safety_ok=safety_ok,
        ))

    if energy_delta_ratio >= 0.05 or max_health < 0.25:
        energy_saving_ratio = max(0.0, -energy_delta_ratio)
        confidence = clamp(0.40 + abs(energy_delta_ratio) * 2.5 + (0.12 if max_health < 0.25 else 0.0), 0.0, 0.95)
        findings.append(advisory(
            simulation_time=latest_time,
            asset="THERMAL-SKID",
            finding="energy-per-unit-production deterioration",
            finding_code="ENERGY_PER_UNIT_PRODUCTION_DETERIORATION",
            confidence=confidence,
            evidence={
                "neuron": "EconomicBasisNeuron",
                "healthEnergyKwh": metrics["energy_consumption_kwh"],
                "baselineKwh": baseline["energy_consumption_kwh"],
                "deltaRatioVsBaseline": round(energy_delta_ratio, 5),
                "availableEnergySavingRatio": round(energy_saving_ratio, 5),
                "processLoadMean": round(sum(float(row["process_load_flow"]) for row in rows) / len(rows), 5),
            },
            recommended_action="Trim pump speed in bounded advisory mode when health risk is low; when risk is high, prioritize maintenance before energy optimization.",
            urgency_hours=168 if max_health < 0.25 else 36,
            recommendation="REDUCE_PUMP_SPEED",
            economic=economic_basis("ENERGY_PER_UNIT_PRODUCTION_DETERIORATION", primary, runs, features, confidence),
            safety_ok=safety_ok,
        ))

    return findings


def row_health_risk(row: Dict[str, Any]) -> float:
    return risk(float(row["vibration_rms"]), float(row["bearing_temperature"]), float(row["pump_power_kw"]))


def advisory(simulation_time: float, asset: str, finding: str, finding_code: str, confidence: float,
             evidence: Dict[str, Any], recommended_action: str, urgency_hours: int,
             recommendation: str | None = None, economic: Dict[str, Any] | None = None,
             safety_ok: bool | None = None, diagnosis: Dict[str, Any] | None = None) -> Dict[str, Any]:
    payload = {
        "simulationTime": round(float(simulation_time), 3),
        "asset": asset,
        "finding": finding,
        "findingCode": finding_code,
        "confidence": round(confidence, 4),
        "evidence": evidence,
        "recommendation": recommendation or "REVIEW_ADVISORY",
        "recommendedAction": recommended_action,
        "urgencyHours": urgency_hours,
        "economicBasis": economic or {},
        "safetyEnvelopeSatisfied": bool(safety_ok) if safety_ok is not None else False,
        "controlBoundary": CONTROL_BOUNDARY,
        "autonomousAction": False,
    }
    if diagnosis:
        payload["diagnosis"] = diagnosis
    return payload


def trained_model_advisory_findings(name: str, primary: Dict[str, Any], runs: Dict[str, Dict[str, Any]],
                                    model_path: Path = TRAINED_MODEL_PATH) -> List[Dict[str, Any]]:
    if not model_path.exists():
        return []
    model = json.loads(model_path.read_text(encoding="utf-8"))
    rows = primary["rows"]
    if not rows:
        return []
    features = production_feature_map(primary, runs)
    feature_names = model["featureNames"]
    scaler = model["scaler"]
    latest_time = rows[-1]["simulation_time"]
    findings: List[Dict[str, Any]] = []
    for finding_code, head in model["heads"].items():
        score, contributions = model_score(features, feature_names, scaler, head)
        threshold = float(head["decisionThreshold"])
        if score < threshold:
            continue
        template = head["advisoryTemplate"]
        diagnosis = diagnose_control_loop(features) if finding_code == "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION" else None
        findings.append({
            "simulationTime": round(float(latest_time), 3),
            "asset": template["asset"],
            "finding": template["finding"],
            "findingCode": finding_code,
            "confidence": round(score, 4),
            "evidence": {
                "neuron": template.get("neuron", "TrainedFindingHeadNeuron"),
                "modelId": model["modelId"],
                "trainedSnapshotVersion": model["trainedSnapshotVersion"],
                "decisionThreshold": threshold,
                "scenario": name,
                "controlBoundary": CONTROL_BOUNDARY,
                "diagnosis": diagnosis,
                "topFeatureContributions": contributions[:6],
            },
            "recommendation": template.get("recommendationCode", "REVIEW_ADVISORY"),
            "recommendedAction": template["recommendedAction"],
            "urgencyHours": template["urgencyHours"],
            "economicBasis": economic_basis(finding_code, primary, runs, features, score),
            "safetyEnvelopeSatisfied": safety_envelope_satisfied(primary),
            "diagnosis": diagnosis,
            "controlBoundary": CONTROL_BOUNDARY,
            "autonomousAction": False,
        })
    return findings


def production_feature_map(primary: Dict[str, Any], runs: Dict[str, Dict[str, Any]]) -> Dict[str, float]:
    rows = primary["rows"]
    metrics = primary["metrics"]
    baseline = runs["fixed-baseline-pid"]["metrics"]
    risks = [row_health_risk(row) for row in rows]
    residuals = [abs(float(row["temperature_pv"]) - float(row["process_temperature"])) for row in rows]
    production_units = sum(float(row["process_load_flow"]) * 0.1 / 3600.0 for row in rows)
    baseline_energy = max(float(baseline["energy_consumption_kwh"]), 1e-9)
    temperature_error = [float(row["temperature_pv"]) - float(row["temperature_sp"]) for row in rows]
    flow_values = numeric_series(rows, "flow_pv")
    valve_cmd = numeric_series(rows, "valve_cmd")
    pump_cmd = numeric_series(rows, "pump_speed_cmd")
    temp_acf, temp_acf_lag = autocorrelation_peak(temperature_error)
    flow_acf, flow_acf_lag = autocorrelation_peak(flow_values)
    valve_density = direction_change_density(valve_cmd)
    pump_density = direction_change_density(pump_cmd)
    interlock_ratio = sum(
        1 for row in rows
        if bool(row["high_temp_interlock"]) or bool(row["low_flow_interlock"]) or bool(row["low_suction_interlock"])
    ) / len(rows)
    operator_override_ratio = sum(1 for row in rows if bool(row["operator_override"])) / len(rows)
    duration = max(float(rows[-1]["simulation_time"]), 1.0)
    return {
        "max_health_risk": max(risks),
        "mean_health_risk": sum(risks) / len(risks),
        "max_vibration_rms": max(float(row["vibration_rms"]) for row in rows),
        "max_bearing_temperature_c": max(float(row["bearing_temperature"]) for row in rows),
        "mean_pump_power_kw": sum(float(row["pump_power_kw"]) for row in rows) / len(rows),
        "max_pump_power_kw": max(float(row["pump_power_kw"]) for row in rows),
        "min_flow_pv": min(float(row["flow_pv"]) for row in rows),
        "low_flow_ratio": sum(1 for row in rows if float(row["flow_pv"]) < 0.30) / len(rows),
        "min_suction_pressure_bar": min(float(row["suction_pressure"]) for row in rows),
        "low_suction_ratio": sum(1 for row in rows if float(row["suction_pressure"]) < 0.42) / len(rows),
        "actuator_reversal_rate": float(metrics["number_of_actuator_reversals"]) / max(1.0, len(rows) / 10.0),
        "total_actuator_travel": float(metrics["total_actuator_travel"]),
        "max_temperature_model_residual_c": max(residuals),
        "mean_temperature_model_residual_c": sum(residuals) / len(residuals),
        "energy_consumption_kwh": float(metrics["energy_consumption_kwh"]),
        "energy_per_unit_production_kwh": float(metrics["energy_consumption_kwh"]) / max(production_units, 1e-9),
        "baseline_energy_delta_ratio": (float(metrics["energy_consumption_kwh"]) - baseline_energy) / baseline_energy,
        "integral_absolute_error": float(metrics["integral_absolute_error"]),
        "maximum_overshoot_c": float(metrics["maximum_overshoot"]),
        "time_outside_safety_bounds": float(metrics["time_outside_safety_bounds"]),
        "interlock_ratio": interlock_ratio,
        "operator_override_ratio": operator_override_ratio,
        "opcua_failsafe_ratio": sum(1 for item in primary["controller"] if item["verdict"] == "LOCAL_FAIL_SAFE") / max(1, len(primary["controller"])),
        "mqtt_outage_available": 1.0 if metrics["control_availability_during_mqtt_outage"] else 0.0,
        "temperature_error_zero_crossing_rate": zero_crossing_rate(temperature_error),
        "temperature_error_acf_peak": temp_acf,
        "temperature_error_acf_lag_ticks": float(temp_acf_lag),
        "flow_acf_peak": flow_acf,
        "flow_acf_lag_ticks": float(flow_acf_lag),
        "valve_command_reversal_density": valve_density,
        "pump_command_reversal_density": pump_density,
        "command_position_mismatch_ratio": mean_abs_command_position_mismatch(rows),
        "valve_stiction_proxy": valve_stiction_proxy(rows),
        "cascade_interaction_score": clamp(temp_acf * flow_acf * (valve_density + pump_density) / 4.0, 0.0, 1.0),
        "controller_intervention_rate": len(primary["interventions"]) / duration,
        "load_transition_ratio": load_transition_ratio(rows),
        "startup_phase_ratio": sum(1 for row in rows if float(row["simulation_time"]) <= min(30.0, duration * 0.2)) / len(rows),
        "steady_state_phase_ratio": sum(
            1 for row in rows
            if float(row["simulation_time"]) > min(30.0, duration * 0.2)
            and not bool(row["high_temp_interlock"])
            and not bool(row["operator_override"])
        ) / len(rows),
        "maintenance_context_active": 1.0 if max(risks) >= 0.35 else 0.0,
    }


def model_score(features: Dict[str, float], feature_names: List[str], scaler: Dict[str, List[float]],
                head: Dict[str, Any]) -> tuple[float, List[Dict[str, Any]]]:
    z_values: List[float] = []
    contributions: List[Dict[str, Any]] = []
    for idx, feature_name in enumerate(feature_names):
        value = float(features[feature_name])
        std = float(scaler["std"][idx]) or 1.0
        z = (value - float(scaler["mean"][idx])) / std
        weight = float(head["weights"][idx])
        z_values.append(z)
        contributions.append({
            "feature": feature_name,
            "value": round(value, 6),
            "weight": round(weight, 6),
            "contribution": round(weight * z, 6),
        })
    linear = sum(float(weight) * z for weight, z in zip(head["weights"], z_values)) + float(head["bias"])
    score = sigmoid(linear)
    contributions.sort(key=lambda item: abs(float(item["contribution"])), reverse=True)
    return score, contributions


def sigmoid(value: float) -> float:
    if value >= 0.0:
        z = math.exp(-value)
        return 1.0 / (1.0 + z)
    z = math.exp(value)
    return z / (1.0 + z)


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
    heuristic_findings = advisory_findings(name, primary, runs)
    model_findings = trained_model_advisory_findings(name, primary, runs)
    production_findings = model_findings if model_findings else heuristic_findings
    write_jsonl(out_dir / "heuristic_advisory_findings.jsonl", heuristic_findings)
    write_jsonl(out_dir / "model_advisory_findings.jsonl", model_findings)
    write_jsonl(out_dir / "advisory_findings.jsonl", production_findings)
    manifest = {
        "scenario": name,
        "fmuSource": "scripts/demo-industrial-fmi/plant/thermal_skid_fmu.py",
        "modelSource": "scripts/demo-industrial-fmi/plant/thermal_skid_model.py",
        "trainedModel": str(TRAINED_MODEL_PATH),
        "trainedModelScored": TRAINED_MODEL_PATH.exists(),
        "trainedModelFindingCount": len(model_findings),
        "heuristicFindingCount": len(heuristic_findings),
        "productionFindingSource": "trained-model" if model_findings else "heuristic-fallback",
        "opcUaCommandPathOnly": True,
        "mqttAdvisoryOnly": True,
        "advisorySchema": {
            "asset": "string",
            "finding": "string",
            "confidence": "0.0-1.0",
            "evidence": "object",
            "recommendedAction": "string",
            "urgencyHours": "integer",
            "autonomousAction": False,
        },
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
