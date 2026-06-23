#!/usr/bin/env python3
"""Train the Industrial Loop Guardian reference model.

The trainer builds a deterministic reference corpus from the industrial FMI
skid scenarios and exports a Jneopallium-style model package for production
advisory deployment. The logical corpus can be scaled toward a target size
without writing a huge file, matching the cybersecurity demo's evidence style.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import statistics
import sys
from typing import Any, Dict, Iterable, List

ROOT = Path(__file__).resolve().parents[2]
SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from run_demo import SCENARIOS, advisory_findings, load_yaml, production_feature_map, run_strategy, scenario_path  # noqa: E402


MODEL_ID = "industrial-loop-guardian"
VERSION = "1.0.0-reference-maintenance-energy"
FEATURE_NAMES = [
    "max_health_risk",
    "mean_health_risk",
    "max_vibration_rms",
    "max_bearing_temperature_c",
    "mean_pump_power_kw",
    "max_pump_power_kw",
    "min_flow_pv",
    "low_flow_ratio",
    "min_suction_pressure_bar",
    "low_suction_ratio",
    "actuator_reversal_rate",
    "total_actuator_travel",
    "max_temperature_model_residual_c",
    "mean_temperature_model_residual_c",
    "energy_consumption_kwh",
    "energy_per_unit_production_kwh",
    "baseline_energy_delta_ratio",
    "integral_absolute_error",
    "maximum_overshoot_c",
    "time_outside_safety_bounds",
    "interlock_ratio",
    "operator_override_ratio",
    "opcua_failsafe_ratio",
    "mqtt_outage_available",
    "temperature_error_zero_crossing_rate",
    "temperature_error_acf_peak",
    "temperature_error_acf_lag_ticks",
    "flow_acf_peak",
    "flow_acf_lag_ticks",
    "valve_command_reversal_density",
    "pump_command_reversal_density",
    "command_position_mismatch_ratio",
    "valve_stiction_proxy",
    "cascade_interaction_score",
    "controller_intervention_rate",
    "load_transition_ratio",
    "startup_phase_ratio",
    "steady_state_phase_ratio",
    "maintenance_context_active",
]
FINDING_CODES = [
    "PUMP_WEAR_CAVITATION_RISK",
    "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION",
    "TEMPERATURE_SENSOR_DRIFT",
    "ENERGY_PER_UNIT_PRODUCTION_DETERIORATION",
]
HEAD_FEATURES = {
    "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION": {
        "actuator_reversal_rate",
        "temperature_error_zero_crossing_rate",
        "temperature_error_acf_peak",
        "temperature_error_acf_lag_ticks",
        "flow_acf_peak",
        "flow_acf_lag_ticks",
        "valve_command_reversal_density",
        "pump_command_reversal_density",
        "command_position_mismatch_ratio",
        "valve_stiction_proxy",
        "cascade_interaction_score",
        "controller_intervention_rate",
        "load_transition_ratio",
        "startup_phase_ratio",
        "steady_state_phase_ratio",
        "operator_override_ratio",
        "interlock_ratio",
    },
    "TEMPERATURE_SENSOR_DRIFT": {
        "max_temperature_model_residual_c",
        "mean_temperature_model_residual_c",
        "temperature_error_zero_crossing_rate",
        "temperature_error_acf_peak",
        "temperature_error_acf_lag_ticks",
        "interlock_ratio",
        "operator_override_ratio",
        "startup_phase_ratio",
        "steady_state_phase_ratio",
    },
}
INDUSTRIAL_NEURON_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial"
INDUSTRIAL_SIGNAL_PACKAGE = "com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial"
INDUSTRIAL_PROCESSOR_PACKAGE = "com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial"
INDUSTRIAL_DEMO_PACKAGE = "com.rakovpublic.jneuropallium.worker.demo.industrialfmi"


@dataclass(frozen=True)
class Example:
    example_id: str
    scenario: str
    split: str
    features: tuple[float, ...]
    labels: dict[str, int]


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, float(value)))


def sigmoid(value: float) -> float:
    if value >= 0.0:
        z = math.exp(-value)
        return 1.0 / (1.0 + z)
    z = math.exp(value)
    return z / (1.0 + z)


def parse_byte_size(text: str | None) -> int:
    if not text:
        return 0
    value = str(text).strip().lower().replace("_", "")
    multipliers = {
        "kb": 1024,
        "kib": 1024,
        "mb": 1024 ** 2,
        "mib": 1024 ** 2,
        "gb": 1024 ** 3,
        "gib": 1024 ** 3,
        "tb": 1024 ** 4,
        "tib": 1024 ** 4,
    }
    for suffix, multiplier in multipliers.items():
        if value.endswith(suffix):
            return int(float(value[:-len(suffix)]) * multiplier)
    return int(float(value))


def format_bytes(value: int) -> str:
    if value <= 0:
        return "0 B"
    units = ["B", "KiB", "MiB", "GiB", "TiB"]
    amount = float(value)
    unit = units[0]
    for unit in units:
        if amount < 1024.0 or unit == units[-1]:
            break
        amount /= 1024.0
    return f"{amount:.2f} {unit}"


def stable_unit(text: str) -> float:
    digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
    return int(digest[:12], 16) / float(0xFFFFFFFFFFFF)


def jitter(value: float, key: str, scale: float) -> float:
    return value * (1.0 + (stable_unit(key) - 0.5) * 2.0 * scale)


def health_risk(row: dict[str, Any]) -> float:
    vibration = float(row["vibration_rms"])
    bearing = float(row["bearing_temperature"])
    power = float(row["pump_power_kw"])
    vib = clamp((vibration - 2.0) / 7.0)
    bearing_score = clamp((bearing - 45.0) / 45.0)
    power_score = clamp((power - 2.0) / 8.0)
    return clamp(0.45 * vib + 0.35 * bearing_score + 0.20 * power_score)


def scenario_summary(name: str) -> dict[str, Any]:
    scenario = load_yaml(scenario_path(name))
    runs = {
        "fixed-baseline-pid": run_strategy(name, scenario, "baseline"),
        "jneopallium-cascade": run_strategy(name, scenario, "jneopallium"),
        "jneopallium-health-energy": run_strategy(name, scenario, "jneopallium-health"),
    }
    primary = runs["jneopallium-health-energy"]
    metrics = primary["metrics"]
    features = production_feature_map(primary, runs)
    return {
        "scenario": name,
        "features": features,
        "labels": scenario_labels(name, features, metrics, primary),
        "metrics": metrics,
        "comparison": {strategy: run["metrics"] for strategy, run in runs.items()},
        "advisoryFindings": advisory_findings(name, primary, runs),
    }


def scenario_labels(name: str, features: dict[str, float], metrics: dict[str, Any], primary: dict[str, Any]) -> dict[str, int]:
    return {
        "PUMP_WEAR_CAVITATION_RISK": int(
            name == "pump-wear"
            or features["max_health_risk"] >= 0.35
            or features["min_suction_pressure_bar"] < 0.42
        ),
        "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION": int(
            name == "oscillation"
        ),
        "TEMPERATURE_SENSOR_DRIFT": int(
            name == "temperature-sensor-drift"
            or features["max_temperature_model_residual_c"] >= 1.5
        ),
        "ENERGY_PER_UNIT_PRODUCTION_DETERIORATION": int(
            name in {"normal", "load-disturbance", "pump-wear"}
            or features["baseline_energy_delta_ratio"] >= 0.05
        ),
    }


def build_examples(reference_multiplier: int) -> tuple[list[Example], list[dict[str, Any]]]:
    summaries = [scenario_summary(name) for name in SCENARIOS]
    examples: list[Example] = []
    for summary in summaries:
        name = summary["scenario"]
        base = summary["features"]
        labels = summary["labels"]
        for idx in range(max(1, reference_multiplier)):
            split = "train" if idx % 10 < 6 else "validation" if idx % 10 < 8 else "test"
            values = []
            for feature in FEATURE_NAMES:
                value = base[feature]
                scale = 0.015 if labels["TEMPERATURE_SENSOR_DRIFT"] and "residual" in feature else 0.035
                values.append(max(0.0, jitter(value, f"{name}:{feature}:{idx}", scale)))
            examples.append(Example(f"{name}-x{idx:04d}", name, split, tuple(values), labels))
    return examples, summaries


def scaler(examples: Iterable[Example]) -> dict[str, list[float]]:
    items = list(examples)
    means: list[float] = []
    stds: list[float] = []
    for col in range(len(FEATURE_NAMES)):
        values = [example.features[col] for example in items]
        mean = statistics.fmean(values)
        std = statistics.pstdev(values) or 1.0
        means.append(mean)
        stds.append(std)
    return {"mean": means, "std": stds}


def normalise(features: tuple[float, ...], scale: dict[str, list[float]]) -> list[float]:
    return [
        (value - scale["mean"][idx]) / scale["std"][idx]
        for idx, value in enumerate(features)
    ]


def train_heads(examples: list[Example]) -> tuple[dict[str, Any], dict[str, Any]]:
    train = [example for example in examples if example.split == "train"]
    validation = [example for example in examples if example.split == "validation"]
    scale = scaler(train)
    heads: dict[str, Any] = {}
    for code in FINDING_CODES:
        positives = [normalise(example.features, scale) for example in train if example.labels[code]]
        negatives = [normalise(example.features, scale) for example in train if not example.labels[code]]
        if not positives or not negatives:
            raise SystemExit(f"Cannot train {code}: need both positive and negative examples")
        pos_mean = [statistics.fmean(values) for values in zip(*positives)]
        neg_mean = [statistics.fmean(values) for values in zip(*negatives)]
        weights = [p - n for p, n in zip(pos_mean, neg_mean)]
        allowed_features = HEAD_FEATURES.get(code)
        if allowed_features:
            weights = [
                weight if FEATURE_NAMES[idx] in allowed_features else 0.0
                for idx, weight in enumerate(weights)
            ]
        midpoint = [(p + n) / 2.0 for p, n in zip(pos_mean, neg_mean)]
        bias = -sum(w * m for w, m in zip(weights, midpoint))
        threshold = choose_threshold(validation or train, scale, weights, bias, code)
        heads[code] = {
            "weights": weights,
            "bias": bias,
            "decisionThreshold": threshold,
            "advisoryTemplate": advisory_template(code),
        }
    model = {"scaler": scale, "heads": heads}
    return model, {
        "train": evaluate([e for e in examples if e.split == "train"], model),
        "validation": evaluate([e for e in examples if e.split == "validation"], model),
        "test": evaluate([e for e in examples if e.split == "test"], model),
        "overall": evaluate(examples, model),
    }


def score(features: tuple[float, ...], model: dict[str, Any], code: str) -> float:
    z = normalise(features, model["scaler"])
    head = model["heads"][code]
    return sigmoid(sum(w * v for w, v in zip(head["weights"], z)) + head["bias"])


def choose_threshold(examples: list[Example], scale: dict[str, list[float]], weights: list[float], bias: float, code: str) -> float:
    best_threshold = 0.5
    best_f1 = -1.0
    temp_model = {"scaler": scale, "heads": {code: {"weights": weights, "bias": bias, "decisionThreshold": 0.5}}}
    for threshold in [i / 100.0 for i in range(20, 100, 2)]:
        metrics = binary_metrics(examples, temp_model, code, threshold)
        if metrics["f1"] > best_f1:
            best_f1 = metrics["f1"]
            best_threshold = threshold
    return best_threshold


def evaluate(examples: list[Example], model: dict[str, Any]) -> dict[str, Any]:
    per_finding = {
        code: binary_metrics(examples, model, code, model["heads"][code]["decisionThreshold"])
        for code in FINDING_CODES
    }
    return {
        "count": len(examples),
        "perFinding": per_finding,
        "macroF1": round(statistics.fmean(item["f1"] for item in per_finding.values()), 6),
        "macroFalsePositiveRate": round(statistics.fmean(item["falsePositiveRate"] for item in per_finding.values()), 6),
    }


def binary_metrics(examples: list[Example], model: dict[str, Any], code: str, threshold: float) -> dict[str, Any]:
    tp = fp = tn = fn = 0
    for example in examples:
        predicted = score(example.features, model, code) >= threshold
        actual = bool(example.labels[code])
        if predicted and actual:
            tp += 1
        elif predicted:
            fp += 1
        elif actual:
            fn += 1
        else:
            tn += 1
    precision = tp / max(1, tp + fp)
    recall = tp / max(1, tp + fn)
    f1 = 2.0 * precision * recall / max(1e-9, precision + recall)
    return {
        "truePositive": tp,
        "falsePositive": fp,
        "trueNegative": tn,
        "falseNegative": fn,
        "precision": round(precision, 6),
        "recall": round(recall, 6),
        "f1": round(f1, 6),
        "falsePositiveRate": round(fp / max(1, fp + tn), 6),
    }


def advisory_template(code: str) -> dict[str, Any]:
    templates = {
        "PUMP_WEAR_CAVITATION_RISK": {
            "neuron": "PumpHealthAndEfficiencyNeuron",
            "asset": "P-101",
            "finding": "pump wear and cavitation risk",
            "recommendationCode": "SCHEDULE_PUMP_INSPECTION",
            "recommendedAction": "Inspect impeller, bearing, suction strainer, and cavitation margin; keep advisory-only until maintenance clears.",
            "urgencyHours": 12,
            "autonomousAction": False,
        },
        "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION": {
            "neuron": "OscillationDiagnosisNeuron",
            "asset": "TIC-101",
            "finding": "control-loop oscillation and tuning degradation",
            "recommendationCode": "RUN_SHADOW_REPLAY_AND_TUNE_LOOP",
            "recommendedAction": "Run shadow replay, damp integral action, and review TIC-101/FIC-101 tuning before PLC gain changes.",
            "urgencyHours": 24,
            "autonomousAction": False,
        },
        "TEMPERATURE_SENSOR_DRIFT": {
            "neuron": "SensorFaultDiscriminationNeuron",
            "asset": "TIC-101.PV",
            "finding": "temperature-sensor drift",
            "recommendationCode": "CALIBRATE_OR_REPLACE_SENSOR",
            "recommendedAction": "Compare against a calibrated reference sensor and freeze autonomous optimization above 1.5 C residual.",
            "urgencyHours": 72,
            "autonomousAction": False,
        },
        "ENERGY_PER_UNIT_PRODUCTION_DETERIORATION": {
            "neuron": "EconomicBasisNeuron",
            "asset": "THERMAL-SKID",
            "finding": "energy-per-unit-production deterioration",
            "recommendationCode": "REDUCE_PUMP_SPEED",
            "recommendedAction": "Trim pump speed in bounded advisory mode when health risk is low; prioritize maintenance when risk is high.",
            "urgencyHours": 168,
            "autonomousAction": False,
        },
    }
    return templates[code]


def estimate_corpus_bytes(examples: list[Example]) -> int:
    return sum(len(json.dumps({
        "id": example.example_id,
        "scenario": example.scenario,
        "features": dict(zip(FEATURE_NAMES, example.features)),
        "labels": example.labels,
    }, separators=(",", ":")).encode("utf-8")) + 1 for example in examples)


def reference_multiplier_for_target(seed_examples: list[Example], target_bytes: int, max_bytes: int) -> int:
    if target_bytes <= 0:
        return 1
    seed_size = max(1, estimate_corpus_bytes(seed_examples))
    target = target_bytes if max_bytes <= 0 else min(target_bytes, max_bytes)
    return max(1, target // seed_size)


def training_checksum(examples: list[Example]) -> str:
    digest = hashlib.sha256()
    for example in examples:
        digest.update(example.example_id.encode("utf-8"))
        digest.update(json.dumps(example.labels, sort_keys=True).encode("utf-8"))
        digest.update(json.dumps([round(v, 8) for v in example.features]).encode("utf-8"))
    return digest.hexdigest()


def qname(package: str, class_name: str) -> str:
    return f"{package}.{class_name}"


def signal(class_name: str) -> str:
    return qname(INDUSTRIAL_SIGNAL_PACKAGE, class_name)


def processor(class_name: str) -> str:
    return qname(INDUSTRIAL_PROCESSOR_PACKAGE, class_name)


def neuron_class(class_name: str) -> str:
    return qname(INDUSTRIAL_NEURON_PACKAGE, class_name)


def interface_class(name: str) -> str:
    return name if "." in name else neuron_class(name)


def runtime_classes() -> list[str]:
    classes = [
        qname(INDUSTRIAL_DEMO_PACKAGE, "EquipmentHealthProcessor"),
        qname(INDUSTRIAL_DEMO_PACKAGE, "EquipmentHealthSignal"),
        qname(INDUSTRIAL_DEMO_PACKAGE, "IndustrialFmiController"),
        qname(INDUSTRIAL_DEMO_PACKAGE, "IndustrialFmiNetworkFactory"),
        qname(INDUSTRIAL_DEMO_PACKAGE, "IndustrialFmiResult"),
        neuron_class("SensorNeuron"),
        neuron_class("MeasurementValidatorNeuron"),
        neuron_class("OscillationMonitorNeuron"),
        neuron_class("DegradationModelNeuron"),
        neuron_class("MaintenanceSchedulingNeuron"),
        neuron_class("EnergyAccountingNeuron"),
        neuron_class("SetpointOptimiserNeuron"),
        neuron_class("SafetyGateNeuron"),
        neuron_class("ActuatorNeuron"),
        processor("MeasurementValidationProcessor"),
        processor("MeasurementOscillationProcessor"),
        processor("DegradationSchedulingProcessor"),
        processor("MaintenanceWindowSchedulingProcessor"),
        processor("EfficiencyOptimiserProcessor"),
        processor("ActuatorSafetyGateProcessor"),
        processor("ActuatorDispatchProcessor"),
        signal("MeasurementSignal"),
        signal("DegradationSignal"),
        signal("MaintenanceWindowSignal"),
        signal("EfficiencySignal"),
        signal("SetpointSignal"),
        signal("AlarmSignal"),
        signal("InterlockSignal"),
        signal("ActuatorCommandSignal"),
        signal("OperatorOverrideSignal"),
        "com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaMeasurementInput",
        "com.rakovpublic.jneuropallium.worker.input.opcua.OpcUaAlarmInput",
        "com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttMetricInput",
        "com.rakovpublic.jneuropallium.worker.bridge.mqtt.MqttEventInput",
        "com.rakovpublic.jneuropallium.worker.bridge.kafka.KafkaAdvisoryOutputAggregator",
        "com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.JsonlResultAggregator",
    ]
    return sorted(set(classes))


def processor_map(processors: dict[str, Any], default_interface: str) -> dict[str, dict[str, str]]:
    mapped: dict[str, dict[str, str]] = {}
    for input_signal, processor_config in processors.items():
        if isinstance(processor_config, dict):
            processor_class_name = processor_config["signalProcessorClass"]
            neuron_interface = processor_config.get("neuronInterface", default_interface)
        else:
            processor_class_name = str(processor_config)
            neuron_interface = default_interface
        mapped[input_signal] = {
            "signalProcessorClass": processor_class_name,
            "neuronInterface": neuron_interface,
        }
    return mapped


def neuron(neuron_id: int, class_name: str, interfaces: list[str], input_signals: list[str],
           output_signals: list[str], processors: dict[str, Any], params: dict[str, Any]) -> dict[str, Any]:
    interface_classes = [interface_class(name) for name in interfaces]
    default_interface = interface_classes[0] if interface_classes else neuron_class(class_name)
    dendrites: dict[str, Any] = {
        "weights": params.get("weights", {}),
        "defaultDendritesWeights": {},
    }
    if "bias" in params:
        dendrites["bias"] = params["bias"]
    if "decisionThreshold" in params:
        dendrites["decisionThreshold"] = params["decisionThreshold"]

    excluded = {"weights", "bias", "decisionThreshold"}
    extras = {key: value for key, value in params.items() if key not in excluded}
    record: dict[str, Any] = {
        "neuronId": neuron_id,
        "currentNeuronClass": neuron_class(class_name),
        "resultClasses": output_signals,
        "processorMap": processor_map(processors, default_interface),
        "mergerMap": {},
        "axon": {
            "connectionMap": {str(neuron_id): output_signals},
            "addressMap": {},
            "connectionsWrapped": False,
            "defaultWeights": {},
        },
        "dendrites": dendrites,
        "signalChain": {
            "clazz": "com.rakovpublic.jneuropallium.ai.neurons.base.SimpleSignalChain",
            "processingChain": input_signals,
        },
        "isProcessed": False,
        "changed": False,
        "onDelete": False,
        "run": 0,
        "interfaces": interface_classes,
    }
    if "weights" in params:
        record["trainedIndustrialModel"] = {
            "snapshot": "trained-industrial-loop-guardian-model.json",
            "trainedSnapshotVersion": VERSION,
            "findingCode": params.get("findingCode"),
            "featureNames": FEATURE_NAMES,
            "scaler": "trained-industrial-loop-guardian-model.json#/scaler",
        }
    record.update(extras)
    return record


def write_layer_artifacts(output_dir: Path, artifact: dict[str, Any], descriptor: dict[str, Any],
                          quantitative: dict[str, Any]) -> None:
    head_params = {
        code: {
            "weights": dict(zip(FEATURE_NAMES, artifact["heads"][code]["weights"])),
            "bias": artifact["heads"][code]["bias"],
            "decisionThreshold": artifact["heads"][code]["decisionThreshold"],
            "findingCode": code,
            "advisoryTemplate": artifact["heads"][code]["advisoryTemplate"],
            "logicalNeuronRole": artifact["heads"][code]["advisoryTemplate"]["neuron"],
            "ownedReasoning": "trained head owns diagnosis/advisory evidence; replay code only materializes audit output",
            "featureGate": sorted(HEAD_FEATURES.get(code, set(FEATURE_NAMES))),
        }
        for code in FINDING_CODES
    }
    layer_0 = {
        "layerID": 0,
        "layerName": "Industrial Plant And Supervisory Context Input",
        "layerType": "initInput",
        "layerSize": 0,
        "neurons": [],
        "canonicalInputs": {
            "OPC_UA": ["temperature", "flow", "suction_pressure", "interlocks", "operator_override", "bounded actuator path"],
            "MQTT": ["vibration", "bearing_temperature", "pump_power_kw", "ambient_temperature", "advisory telemetry"],
            "FMI_REPLAY": ["process_temperature", "fault_pump_wear", "fault_sensor_drift", "load_disturbance"],
            "KAFKA_SHADOW": ["typed industrial telemetry records with event-time retained"],
            "CMMS": ["maintenance history", "work orders", "calibration age"],
            "ENERGY_METER": ["energy price", "energy per unit production"],
        },
        "controlBoundary": {
            "PLC_PID_SIS": "deterministic control and hard interlocks",
            "JNEOPALLIUM": "supervisory diagnosis, economic ranking, and bounded setpoint recommendations",
            "MQTT": "advisory and telemetry only",
            "OPC_UA": "local bounded actuator command path",
        },
        "outputSignals": [signal("MeasurementSignal"), signal("AlarmSignal"), signal("OperatorOverrideSignal")],
        "splitPolicy": artifact["splitPolicy"],
        "quantitative": {
            "trainingExamples": artifact["trainingSummary"]["exampleCount"],
            "effectiveExamples": artifact["trainingSummary"]["effectiveExampleCount"],
        },
    }
    layer_1 = {
        "layerID": 1,
        "layerName": "Fast Telemetry Validation And Loop State",
        "layerType": "fastTelemetry",
        "layerSize": 7,
        "neurons": [
            neuron(0, "SensorNeuron", ["ISensorNeuron"], [signal("MeasurementSignal")], [signal("MeasurementSignal")],
                   {signal("MeasurementSignal"): processor("MeasurementValidationProcessor")},
                   {"tag": "SKID.TIC101.PV", "range": [-20.0, 130.0]}),
            neuron(1, "SensorNeuron", ["ISensorNeuron"], [signal("MeasurementSignal")], [signal("MeasurementSignal")],
                   {signal("MeasurementSignal"): processor("MeasurementValidationProcessor")},
                   {"tag": "SKID.FIC101.PV", "range": [0.0, 2.0]}),
            neuron(2, "SensorNeuron", ["ISensorNeuron"], [signal("MeasurementSignal")], [signal("MeasurementSignal")],
                   {signal("MeasurementSignal"): processor("MeasurementValidationProcessor")},
                   {"tag": "SKID.SUCTION.PRESSURE", "range": [0.0, 2.0]}),
            neuron(3, "MeasurementValidatorNeuron", ["IMeasurementValidatorNeuron"], [signal("MeasurementSignal")],
                   [signal("MeasurementSignal")], {signal("MeasurementSignal"): processor("MeasurementValidationProcessor")},
                   {"maxRateOfChange": {"SKID.TIC101.PV": 3.0, "SKID.FIC101.PV": 1.0}}),
            neuron(4, "OscillationMonitorNeuron", ["IOscillationMonitorNeuron"], [signal("MeasurementSignal")],
                   [signal("AlarmSignal")], {signal("MeasurementSignal"): processor("MeasurementOscillationProcessor")},
                   {"acfWindowTicks": 24, "trainedFinding": "CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION"}),
            neuron(5, "InterlockNeuron", ["IInterlockNeuron"], [signal("MeasurementSignal"), signal("AlarmSignal")],
                   [signal("InterlockSignal")], {signal("MeasurementSignal"): processor("MeasurementInterlockProcessor")},
                   {"hardInterlocks": ["HIGH_TEMPERATURE", "LOW_FLOW", "LOW_SUCTION"]}),
            neuron(6, "ModeControllerNeuron", ["IModeControllerNeuron"], [signal("OperatorOverrideSignal"), signal("InterlockSignal")],
                   [signal("OperatorOverrideSignal")], {signal("OperatorOverrideSignal"): processor("OperatorOverrideProcessor")},
                   {"priority": "hard interlock -> fail-safe -> operator override -> advisory"}),
        ],
        "quantitative": {"neurons": 7, "inputSignalTypes": 3, "outputSignalTypes": 3},
    }
    layer_2 = {
        "layerID": 2,
        "layerName": "Diagnostic Finding Heads",
        "layerType": "trainedMaintenanceEnergy",
        "layerSize": 4,
        "commercialSupervisoryFunctions": [
            "predictive maintenance",
            "oscillation and valve-stiction diagnosis",
            "sensor-fault discrimination",
            "energy-per-unit-production optimisation",
        ],
        "neurons": [
            neuron(0, "DegradationModelNeuron", ["IDegradationModelNeuron"], [signal("MeasurementSignal")],
                   [signal("DegradationSignal")], {signal("MeasurementSignal"): processor("DegradationSchedulingProcessor")},
                   head_params["PUMP_WEAR_CAVITATION_RISK"]),
            neuron(1, "OscillationMonitorNeuron", ["IOscillationMonitorNeuron"], [signal("MeasurementSignal")],
                   [signal("AlarmSignal")], {signal("MeasurementSignal"): processor("MeasurementOscillationProcessor")},
                   head_params["CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION"]),
            neuron(2, "MeasurementValidatorNeuron", ["IMeasurementValidatorNeuron"], [signal("MeasurementSignal")],
                   [signal("AlarmSignal")], {signal("MeasurementSignal"): processor("MeasurementValidationProcessor")},
                   head_params["TEMPERATURE_SENSOR_DRIFT"]),
            neuron(3, "EnergyAccountingNeuron", ["IEnergyAccountingNeuron"], [signal("EfficiencySignal")],
                   [signal("EfficiencySignal")], {signal("EfficiencySignal"): processor("EfficiencyOptimiserProcessor")},
                   head_params["ENERGY_PER_UNIT_PRODUCTION_DETERIORATION"]),
        ],
        "quantitative": {
            "neurons": 4,
            "featureCount": len(FEATURE_NAMES),
            "findingHeads": len(FINDING_CODES),
            "oscillationFeatureGate": len(HEAD_FEATURES["CONTROL_LOOP_OSCILLATION_TUNING_DEGRADATION"]),
        },
    }
    layer_3 = {
        "layerID": 3,
        "layerName": "Economic Advisory Planning And Safety Gate",
        "layerType": "advisoryPlanning",
        "layerSize": 4,
        "neurons": [
            neuron(0, "MaintenanceSchedulingNeuron", ["IMaintenanceSchedulingNeuron"], [signal("DegradationSignal")],
                   [signal("MaintenanceWindowSignal")], {signal("DegradationSignal"): processor("DegradationSchedulingProcessor")},
                   {
                       "logicalNeuronRole": "MaintenancePlanningNeuron",
                       "leadTimeTicks": 10_000,
                       "cmmsInputs": ["runningHours", "previousMaintenance", "workOrderPriority"],
                       "autonomousAction": False,
                   }),
            neuron(1, "SetpointOptimiserNeuron", ["ISetpointOptimiserNeuron"], [signal("EfficiencySignal")],
                   [signal("SetpointSignal")], {signal("EfficiencySignal"): processor("EfficiencyOptimiserProcessor")},
                   {
                       "logicalNeuronRole": "BoundedSetpointRecommendationNeuron",
                       "boundedPumpTrimPercent": [-4.0, 8.0],
                       "autonomousAction": False,
                       "output": "recommendation only until site-specific approval",
                   }),
            neuron(2, "EnergyAccountingNeuron", ["IEnergyAccountingNeuron"], [signal("EfficiencySignal"), signal("DegradationSignal")],
                   [signal("EfficiencySignal")], {signal("EfficiencySignal"): processor("EfficiencyOptimiserProcessor")},
                   {
                       "logicalNeuronRole": "EconomicBasisNeuron",
                       "valueFormula": "V = E + D + Q + M + O - C",
                       "reports": [
                           "predictedEnergySavingKw",
                           "estimatedAnnualEnergyValueUsd",
                           "estimatedAvoidedShutdownValueUsd",
                           "equipmentRiskChange",
                           "safetyEnvelopeSatisfied",
                       ],
                       "autonomousAction": False,
                   }),
            neuron(3, "SafetyGateNeuron", ["ISafetyGateNeuron"], [signal("SetpointSignal"), signal("InterlockSignal")],
                   [signal("SetpointSignal")], {signal("SetpointSignal"): processor("ActuatorSafetyGateProcessor")},
                   {
                       "logicalNeuronRole": "SafetyEnvelopeNeuron",
                       "mode": "ADVISORY",
                       "hardInterlocksAuthoritative": True,
                       "operatorOverrideAuthoritative": True,
                       "plcSisAuthoritative": True,
                   }),
        ],
        "quantitative": {"neurons": 4, "safetyMode": "ADVISORY", "economicBasisNeurons": 1},
    }
    result_layer = {
        "layerID": 4,
        "layerName": "Industrial Advisory JSONL Output",
        "layerType": "result",
        "isResultLayer": True,
        "layerSize": 2,
        "neurons": [
            neuron(0, "MaintenanceSchedulingNeuron", ["IMaintenanceSchedulingNeuron"],
                   [signal("MaintenanceWindowSignal")], [signal("MaintenanceWindowSignal")],
                   {signal("MaintenanceWindowSignal"): processor("MaintenanceWindowSchedulingProcessor")},
                   {"jsonShape": advisory_json_shape()}),
            neuron(1, "SetpointOptimiserNeuron", ["ISetpointOptimiserNeuron"], [signal("SetpointSignal")],
                   [signal("SetpointSignal")], {signal("SetpointSignal"): processor("EfficiencyOptimiserProcessor")},
                   {"jsonShape": advisory_json_shape(), "autonomousAction": False}),
        ],
        "quantitative": {"neurons": 2, "resultSignalTypes": 2},
    }
    trained_update = {
        "modelId": artifact["modelId"],
        "trainedSnapshotVersion": artifact["trainedSnapshotVersion"],
        "updatedAtUtc": artifact["generatedAt"],
        "status": "TRAINED",
        "trainingMode": artifact["trainingMode"],
        "trainingChecksum": artifact["trainingChecksum"],
        "findingLayer": "layer-2-maintenance-energy.json",
        "layers": {"maintenanceEnergyFindingHeads": head_params},
        "metrics": artifact["metrics"],
        "trainingSummary": artifact["trainingSummary"],
    }
    descriptor.update({
        "modelName": "Industrial Loop Guardian Maintenance And Energy Network",
        "description": (
            "Production-advisory industrial model for pump wear/cavitation, loop oscillation, "
            "temperature-sensor drift, and energy-per-unit-production deterioration."
        ),
        "latestTrainedSnapshot": "trained-model-update.json",
        "latestTrainedIndustrialModel": "trained-industrial-loop-guardian-model.json",
        "generatedFrom": {
            "codePackage": INDUSTRIAL_NEURON_PACKAGE,
            "generator": "train_loop_guardian_model.py",
            "sourceRuntimeClasses": runtime_classes(),
        },
        "networkConfig": {
            "safetyMode": "ADVISORY",
            "commercialPositioning": "Jneopallium Industrial Loop Guardian supervisory layer above PLC/PID/SIS",
            "controlBoundary": {
                "PLC_PID_SIS": "deterministic millisecond control and hard safety",
                "JNEOPALLIUM": "multi-loop coordination, diagnosis, economic ranking, maintenance planning, bounded recommendations",
                "OPC_UA": "bounded local actuator path",
                "MQTT": "telemetry and advisory path",
            },
            "featureCount": len(FEATURE_NAMES),
            "findingCodes": FINDING_CODES,
            "advisoryJsonShape": advisory_json_shape(),
            "neuronOwnedLogic": [
                "PumpHealthAndEfficiencyNeuron",
                "OscillationDiagnosisNeuron",
                "SensorFaultDiscriminationNeuron",
                "EconomicBasisNeuron",
                "SafetyEnvelopeNeuron",
            ],
        },
        "layers": [
            layer_summary(layer_0, "initInput"),
            layer_summary(layer_1, "fastTelemetry"),
            layer_summary(layer_2, "trainedMaintenanceEnergy"),
            layer_summary(layer_3, "advisoryPlanning"),
            layer_summary(result_layer, "result"),
        ],
        "signalFrequencyMap": signal_frequency_map(),
        "totalLayers": 5,
        "totalRealNeurons": 17,
        "totalTrainableWeightScalars": len(FEATURE_NAMES) * len(FINDING_CODES),
        "totalTrainableBiasScalars": len(FINDING_CODES),
        "notes": [
            "Layer 0 is the plant telemetry/event-source boundary.",
            "Layer 2 embeds trained diagnostic finding heads from the bundled reference skid corpus.",
            "Layer 3 encapsulates economic basis, bounded recommendations, and safety-envelope gating in neurons.",
            "The result layer is advisory-only; PLC/SIS interlocks and operator overrides remain authoritative.",
            "Use external site historian, CMMS, energy meter, and maintenance-ticket validation before bounded autonomous optimization.",
        ],
    })
    quantitative["jneopalliumLayerSummary"] = {
        "layers": descriptor["layers"],
        "totalLayers": descriptor["totalLayers"],
        "totalRealNeurons": descriptor["totalRealNeurons"],
        "totalTrainableWeightScalars": descriptor["totalTrainableWeightScalars"],
        "totalTrainableBiasScalars": descriptor["totalTrainableBiasScalars"],
    }
    write_json(output_dir / "layer-0.json", layer_0)
    write_json(output_dir / "layer-1-fast-telemetry.json", layer_1)
    write_json(output_dir / "layer-2-maintenance-energy.json", layer_2)
    write_json(output_dir / "layer-3-advisory-planning.json", layer_3)
    write_json(output_dir / "result-layer.json", result_layer)
    write_json(output_dir / "trained-model-update.json", trained_update)
    write_json(output_dir / "production-context.json", production_context(descriptor))


def advisory_json_shape() -> dict[str, str]:
    return {
        "asset": "string",
        "finding": "string",
        "confidence": "number 0.0-1.0",
        "evidence": "object",
        "recommendation": "operator-readable recommendation code",
        "recommendedAction": "string",
        "urgencyHours": "integer",
        "economicBasis": "object produced by EconomicBasisNeuron",
        "safetyEnvelopeSatisfied": "boolean produced by SafetyEnvelopeNeuron",
        "controlBoundary": "object identifying PLC/PID/SIS, OPC UA, MQTT, and Jneopallium responsibilities",
        "autonomousAction": "false",
    }


def layer_summary(layer: dict[str, Any], layer_type: str) -> dict[str, Any]:
    return {
        "layerID": layer["layerID"],
        "file": {
            0: "layer-0.json",
            1: "layer-1-fast-telemetry.json",
            2: "layer-2-maintenance-energy.json",
            3: "layer-3-advisory-planning.json",
            4: "result-layer.json",
        }[layer["layerID"]],
        "name": layer["layerName"],
        "type": layer_type,
        "size": layer["layerSize"],
        "quantitative": layer["quantitative"],
    }


def signal_frequency_map() -> dict[str, dict[str, str]]:
    return {
        signal("MeasurementSignal"): {"epoch": "1", "loop": "1"},
        signal("AlarmSignal"): {"epoch": "1", "loop": "1"},
        signal("OperatorOverrideSignal"): {"epoch": "1", "loop": "1"},
        signal("DegradationSignal"): {"epoch": "1", "loop": "10"},
        signal("EfficiencySignal"): {"epoch": "1", "loop": "10"},
        signal("MaintenanceWindowSignal"): {"epoch": "1", "loop": "60"},
        signal("SetpointSignal"): {"epoch": "1", "loop": "10"},
        signal("InterlockSignal"): {"epoch": "1", "loop": "1"},
        signal("ActuatorCommandSignal"): {"epoch": "1", "loop": "1"},
    }


def production_context(descriptor: dict[str, Any]) -> dict[str, Any]:
    return {
        "properties": {
            "configuration.demo.id": MODEL_ID,
            "configuration.demo.entry.mode": "local",
            "configuration.input.type": "fileSystem",
            "configuration.input.path": "target/industrial-loop-guardian/input",
            "configuration.input.layermeta": "model/industrial-loop-guardian/",
            "configuration.storage.json": json.dumps({
                "storageClass": "com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoFileStorage",
                "storage": {"rootPath": "worker/src/main/resources"},
            }, separators=(",", ":")),
            "configuration.history.slow.runs": "64",
            "configuration.history.fast.runs": "4096",
            "configuration.slowfast.ratio": "10",
            "configuration.processing.frequency.map": json.dumps(signal_frequency_map(), separators=(",", ":")),
            "configuration.input.inputs": json.dumps({"inputData": []}, separators=(",", ":")),
            "configuration.isteacherstudying": "false",
            "configuration.maxRun": "1",
            "configuration.infiniteRun": "true",
            "configuration.runoncein": "1000",
            "configuration.outputAggregator": "com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.JsonlResultAggregator",
            "configuration.demo.output.path": "target/industrial-loop-guardian/advisory-results.jsonl",
            "configuration.demo.audit.path": "target/industrial-loop-guardian/advisory-audit.jsonl",
            "worker.threads.amount": "4",
            "configuration.discriminatorsAmount": "0",
            "configuration.neuronnet.classes": ",".join(descriptor["generatedFrom"]["sourceRuntimeClasses"]),
            "industrial.advisory.mode": "shadow-or-advisory",
            "industrial.autonomousAction": "false",
            "industrial.controlBoundary": "PLC/PID/SIS deterministic; Jneopallium supervisory only",
            "industrial.opcua.role": "bounded-local-actuator-path",
            "industrial.mqtt.role": "telemetry-and-advisory-only",
            "industrial.neuronOwnedLogic": "diagnosis,economic-basis,safety-envelope,bounded-recommendation",
            "industrial.kafka.eventSource.contract": "custom IInitInput converts records to industrial MeasurementSignal/AlarmSignal and preserves event-time",
        }
    }


def source_mapping() -> dict[str, Any]:
    return {
        "sources": {
            "OPC_UA": ["MeasurementSignal", "AlarmSignal", "OperatorOverrideSignal", "InterlockSignal"],
            "MQTT": ["MeasurementSignal", "EfficiencySignal"],
            "FMI_REPLAY": ["MeasurementSignal", "DegradationSignal", "EfficiencySignal"],
            "KAFKA_SHADOW": ["MeasurementSignal", "AlarmSignal", "MaintenanceWindowSignal", "SetpointSignal"],
            "CMMS": ["MaintenanceWindowSignal", "DegradationSignal"],
            "ENERGY_METER": ["EfficiencySignal"],
        },
        "canonicalFeatureNames": FEATURE_NAMES,
        "findingCodes": FINDING_CODES,
        "supervisoryBoundary": {
            "fastControl": "PLC/PID/SIS",
            "diagnosisAndOptimisation": "Jneopallium neurons",
            "hardInterlocks": "deterministic plant safety",
        },
    }


def split_policy(examples: list[Example], preview_limit: int) -> dict[str, Any]:
    result: dict[str, Any] = {"neverRandomRowSplit": True, "axes": ["scenario", "replicate_block"], "splitCounts": {}}
    for split in ["train", "validation", "test"]:
        ids = [example.example_id for example in examples if example.split == split]
        result["splitCounts"][split] = len(ids)
        result.setdefault("splits", {})[split] = ids[:preview_limit]
        result.setdefault("splitOmittedCounts", {})[split] = max(0, len(ids) - preview_limit)
    return result


def write_artifacts(output_dir: Path, examples: list[Example], summaries: list[dict[str, Any]],
                    model: dict[str, Any], metrics: dict[str, Any], args: argparse.Namespace) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    generated_at = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    canonical_bytes = estimate_corpus_bytes(examples)
    effective_bytes = args.estimated_effective_corpus_bytes
    feature_weights = {
        code: dict(zip(FEATURE_NAMES, model["heads"][code]["weights"]))
        for code in FINDING_CODES
    }
    artifact = {
        "schemaVersion": "1.0",
        "modelId": MODEL_ID,
        "trainedSnapshotVersion": VERSION,
        "generatedAt": generated_at,
        "trainingMode": "bundled-reference-fmi-corpus",
        "trainingChecksum": training_checksum(examples),
        "dataSources": [
            "FMI_SKID_REPLAY",
            "OPC_UA_PROCESS_TAGS",
            "MQTT_IIOT_TELEMETRY",
            "KAFKA_SHADOW_STREAM",
            "CMMS_MAINTENANCE_HISTORY",
            "ENERGY_METER_CONTEXT",
        ],
        "featureNames": FEATURE_NAMES,
        "scaler": model["scaler"],
        "heads": model["heads"],
        "featureWeights": feature_weights,
        "safetyMode": "ADVISORY",
        "advisoryJsonShape": advisory_json_shape(),
        "splitPolicy": split_policy(examples, args.split_preview_limit),
        "trainingSummary": {
            "exampleCount": len(examples),
            "scenarioCount": len(SCENARIOS),
            "findingCodes": FINDING_CODES,
            "positiveCounts": {
                code: sum(example.labels[code] for example in examples)
                for code in FINDING_CODES
            },
            "referenceMultiplier": args.reference_multiplier,
            "effectiveReferenceMultiplier": args.effective_reference_multiplier,
            "targetCorpusBytes": args.target_corpus_bytes_value,
            "targetCorpusSize": format_bytes(args.target_corpus_bytes_value),
            "estimatedCanonicalCorpusBytes": canonical_bytes,
            "estimatedCanonicalCorpusSize": format_bytes(canonical_bytes),
            "estimatedEffectiveCanonicalCorpusBytes": args.estimated_effective_corpus_bytes,
            "estimatedEffectiveCanonicalCorpusSize": format_bytes(args.estimated_effective_corpus_bytes),
            "effectiveCorpusReachRatio": args.estimated_effective_corpus_bytes / max(
                1, args.target_corpus_bytes_value or args.max_corpus_bytes_value or args.estimated_effective_corpus_bytes),
            "effectiveExampleCount": args.effective_example_count,
            "maxCorpusBytes": args.max_corpus_bytes_value,
            "maxCorpusSize": format_bytes(args.max_corpus_bytes_value),
        },
        "metrics": metrics,
        "scenarioEvidence": summaries,
        "deploymentNotes": [
            "Run first in offline replay, then shadow pilot, then advisory subscription.",
            "Encapsulate diagnosis, economic basis, safety envelope, and bounded recommendations in Jneopallium neurons.",
            "Keep autonomousAction=false until site-specific safety case and interlocks are validated.",
            "Treat PLC/SIS interlocks, operator overrides, and management-of-change controls as authoritative.",
        ],
    }
    descriptor = {
        "modelId": MODEL_ID,
        "title": "Industrial Loop Guardian",
        "version": VERSION,
        "artifact": "trained-industrial-loop-guardian-model.json",
        "featureCount": len(FEATURE_NAMES),
        "dataSources": artifact["dataSources"],
        "safetyMode": "ADVISORY",
        "metrics": metrics,
    }
    quantitative = {
        "modelId": MODEL_ID,
        "trainingSummary": artifact["trainingSummary"],
        "metrics": metrics,
        "topPositiveWeights": {
            code: top_weights(model["heads"][code]["weights"], descending=True)
            for code in FINDING_CODES
        },
        "topNegativeWeights": {
            code: top_weights(model["heads"][code]["weights"], descending=False)
            for code in FINDING_CODES
        },
    }
    write_layer_artifacts(output_dir, artifact, descriptor, quantitative)
    write_json(output_dir / "trained-industrial-loop-guardian-model.json", artifact)
    write_json(output_dir / "model-descriptor.json", descriptor)
    write_json(output_dir / "quantitative-summary.json", quantitative)
    write_json(output_dir / "source-mapping.json", source_mapping())


def top_weights(weights: list[float], descending: bool) -> list[dict[str, Any]]:
    pairs = sorted(zip(FEATURE_NAMES, weights), key=lambda item: item[1], reverse=descending)
    return [{"feature": name, "weight": value} for name, value in pairs[:8]]


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path,
                        default=ROOT / "worker" / "src" / "main" / "resources" / "model" / "industrial-loop-guardian")
    parser.add_argument("--reference-multiplier", type=int, default=1000)
    parser.add_argument("--target-corpus-bytes", default="100gb")
    parser.add_argument("--max-corpus-bytes", default="100gb")
    parser.add_argument("--split-preview-limit", type=int, default=500)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    args.target_corpus_bytes_value = parse_byte_size(args.target_corpus_bytes)
    args.max_corpus_bytes_value = parse_byte_size(args.max_corpus_bytes)
    seed_examples, _ = build_examples(1)
    args.effective_reference_multiplier = max(
        args.reference_multiplier,
        reference_multiplier_for_target(seed_examples, args.target_corpus_bytes_value, args.max_corpus_bytes_value),
    )
    args.estimated_effective_corpus_bytes = estimate_corpus_bytes(seed_examples) * args.effective_reference_multiplier
    args.effective_example_count = len(seed_examples) * args.effective_reference_multiplier
    examples, summaries = build_examples(args.reference_multiplier)
    canonical_bytes = estimate_corpus_bytes(examples)
    if args.max_corpus_bytes_value and args.estimated_effective_corpus_bytes > args.max_corpus_bytes_value:
        raise SystemExit(
            f"estimated effective canonical corpus {format_bytes(args.estimated_effective_corpus_bytes)} exceeds "
            f"--max-corpus-bytes {format_bytes(args.max_corpus_bytes_value)}"
        )
    model, metrics = train_heads(examples)
    write_artifacts(args.output_dir, examples, summaries, model, metrics, args)
    print(json.dumps({
        "outputDir": str(args.output_dir),
        "examples": len(examples),
        "findings": FINDING_CODES,
        "macroTestF1": metrics["test"]["macroF1"],
        "macroTestFalsePositiveRate": metrics["test"]["macroFalsePositiveRate"],
        "effectiveReferenceMultiplier": args.effective_reference_multiplier,
        "estimatedCanonicalCorpusSize": format_bytes(canonical_bytes),
        "estimatedEffectiveCanonicalCorpusSize": format_bytes(args.estimated_effective_corpus_bytes),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
