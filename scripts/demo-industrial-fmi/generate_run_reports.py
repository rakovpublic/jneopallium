#!/usr/bin/env python3
"""Generate Industrial Loop Guardian training and production run reports."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MODEL_DIR = ROOT / "worker" / "src" / "main" / "resources" / "model" / "industrial-loop-guardian"
DEFAULT_RUN_DIR = ROOT / "target" / "jneopallium-industrial-fmi"
DEFAULT_OUTPUT_DIR = ROOT / "target" / "industrial-loop-guardian" / "reports"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def evidence_row(path: Path, root: Path) -> dict[str, Any]:
    return {
        "path": str(path.resolve()),
        "relativePath": str(path.resolve().relative_to(root.resolve())) if path.resolve().is_relative_to(root.resolve()) else str(path.resolve()),
        "bytes": path.stat().st_size,
        "sha256": sha256(path),
    }


def write_training_report(model_dir: Path, output_dir: Path) -> Path:
    descriptor = load_json(model_dir / "model-descriptor.json")
    trained = load_json(model_dir / "trained-industrial-loop-guardian-model.json")
    update = load_json(model_dir / "trained-model-update.json")
    quantitative = load_json(model_dir / "quantitative-summary.json")
    context = load_json(model_dir / "production-context.json")
    summary = trained["trainingSummary"]
    metrics = trained["metrics"]

    evidence_files = [
        model_dir / "trained-industrial-loop-guardian-model.json",
        model_dir / "trained-model-update.json",
        model_dir / "model-descriptor.json",
        model_dir / "quantitative-summary.json",
        model_dir / "source-mapping.json",
        model_dir / "production-context.json",
        model_dir / "layer-0.json",
        model_dir / "layer-1-fast-telemetry.json",
        model_dir / "layer-2-maintenance-energy.json",
        model_dir / "layer-3-advisory-planning.json",
        model_dir / "result-layer.json",
    ]
    evidence = [evidence_row(path, ROOT) for path in evidence_files if path.exists()]
    write_json(output_dir / "training-evidence-manifest.json", {"evidence": evidence})

    lines = [
        "# Industrial Loop Guardian Training Run Report",
        "",
        f"- Model ID: `{descriptor['modelId']}`",
        f"- Version: `{descriptor['version']}`",
        f"- Status: `{update['status']}`",
        f"- Training mode: `{trained['trainingMode']}`",
        f"- Training checksum: `{trained['trainingChecksum']}`",
        f"- Generated at: `{trained['generatedAt']}`",
        f"- Effective corpus: `{summary['estimatedEffectiveCanonicalCorpusSize']}` "
        f"({summary['estimatedEffectiveCanonicalCorpusBytes']} bytes)",
        f"- Effective corpus reach ratio: `{summary['effectiveCorpusReachRatio']:.9f}`",
        f"- Reference multiplier: `{summary['referenceMultiplier']}`",
        f"- Effective reference multiplier: `{summary['effectiveReferenceMultiplier']}`",
        f"- Effective example count: `{summary['effectiveExampleCount']}`",
        f"- Safety mode: `{descriptor['safetyMode']}`",
        f"- Tick pacing: `{context['properties'].get('configuration.runoncein')} ms`",
        "",
        "## Metrics",
        "",
        "| Split | Count | Macro F1 | Macro false-positive rate |",
        "| --- | ---: | ---: | ---: |",
    ]
    for split in ["train", "validation", "test", "overall"]:
        m = metrics[split]
        lines.append(f"| {split} | {m['count']} | {m['macroF1']} | {m['macroFalsePositiveRate']} |")
    lines.extend([
        "",
        "## Finding Heads",
        "",
        "| Finding | Test precision | Test recall | Test F1 | Test false-positive rate |",
        "| --- | ---: | ---: | ---: | ---: |",
    ])
    for finding, item in metrics["test"]["perFinding"].items():
        lines.append(
            f"| `{finding}` | {item['precision']} | {item['recall']} | "
            f"{item['f1']} | {item['falsePositiveRate']} |")
    lines.extend([
        "",
        "## Evidence Files",
        "",
        "| File | Bytes | SHA-256 |",
        "| --- | ---: | --- |",
    ])
    for item in evidence:
        lines.append(f"| `{item['relativePath']}` | {item['bytes']} | `{item['sha256']}` |")
    lines.extend([
        "",
        "## Top Training Signals",
        "",
    ])
    for finding, weights in quantitative["topPositiveWeights"].items():
        top = ", ".join(f"`{row['feature']}`={row['weight']:.4f}" for row in weights[:4])
        lines.append(f"- `{finding}`: {top}")

    output = output_dir / "training-run-report.md"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return output


def write_production_report(run_dir: Path, model_dir: Path, output_dir: Path) -> Path:
    context = load_json(model_dir / "production-context.json")
    scenarios = sorted(path for path in run_dir.iterdir() if path.is_dir())
    scenario_rows: list[dict[str, Any]] = []
    evidence_files: list[Path] = []
    finding_counts: dict[str, int] = {}

    for scenario_dir in scenarios:
        metrics_path = scenario_dir / "metrics.json"
        comparison_path = scenario_dir / "comparison.json"
        findings_path = scenario_dir / "advisory_findings.jsonl"
        model_findings_path = scenario_dir / "model_advisory_findings.jsonl"
        heuristic_findings_path = scenario_dir / "heuristic_advisory_findings.jsonl"
        manifest_path = scenario_dir / "manifest.json"
        for path in [metrics_path, comparison_path, findings_path, model_findings_path, heuristic_findings_path, manifest_path]:
            if path.exists():
                evidence_files.append(path)
        if not metrics_path.exists():
            continue
        metrics = load_json(metrics_path)
        findings = load_jsonl(findings_path)
        model_findings = load_jsonl(model_findings_path)
        heuristic_findings = load_jsonl(heuristic_findings_path)
        for finding in findings:
            finding_counts[finding["findingCode"]] = finding_counts.get(finding["findingCode"], 0) + 1
        recommendations = [item.get("recommendation", "REVIEW_ADVISORY") for item in findings]
        annual_value = sum(
            float((item.get("economicBasis") or {}).get("estimatedAnnualEnergyValueUsd", 0.0))
            + float((item.get("economicBasis") or {}).get("estimatedAvoidedShutdownValueUsd", 0.0))
            + float((item.get("economicBasis") or {}).get("estimatedAvoidedFalseShutdownValueUsd", 0.0))
            for item in findings
        )
        safety_blocked = sum(1 for item in findings if not bool(item.get("safetyEnvelopeSatisfied", False)))
        scenario_rows.append({
            "scenario": scenario_dir.name,
            "energy": metrics["energy_consumption_kwh"],
            "overshoot": metrics["maximum_overshoot"],
            "outsideSafety": metrics["time_outside_safety_bounds"],
            "faultDelay": metrics["fault_detection_delay"],
            "mqttAvailability": metrics["control_availability_during_mqtt_outage"],
            "findings": len(findings),
            "modelFindings": len(model_findings),
            "heuristicFindings": len(heuristic_findings),
            "findingCodes": ", ".join(f"`{item['findingCode']}`" for item in findings) or "-",
            "recommendations": ", ".join(f"`{item}`" for item in recommendations) or "-",
            "annualValue": round(annual_value, 2),
            "safetyBlocked": safety_blocked,
        })

    evidence = [evidence_row(path, ROOT) for path in sorted(evidence_files)]
    write_json(output_dir / "production-run-evidence-manifest.json", {
        "runDir": str(run_dir.resolve()),
        "contextRunOnceInMs": context["properties"].get("configuration.runoncein"),
        "evidence": evidence,
        "findingCounts": finding_counts,
    })

    lines = [
        "# Industrial Loop Guardian Production Run Report",
        "",
        f"- Run directory: `{run_dir.resolve()}`",
        f"- Production context: `{(model_dir / 'production-context.json').resolve()}`",
        f"- Tick pacing: `{context['properties'].get('configuration.runoncein')} ms`",
        f"- Advisory mode: `{context['properties'].get('industrial.advisory.mode')}`",
        f"- Autonomous action: `{context['properties'].get('industrial.autonomousAction')}`",
        f"- Scenarios executed: `{len(scenario_rows)}`",
        f"- Total advisory findings: `{sum(row['findings'] for row in scenario_rows)}`",
        "",
        "## Scenario Evidence",
        "",
        "| Scenario | Energy kWh | Max overshoot C | Outside safety s | Fault delay s | MQTT availability | Findings | Finding codes |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |",
    ]
    for row in scenario_rows:
        lines.append(
            f"| `{row['scenario']}` | {row['energy']} | {row['overshoot']} | "
            f"{row['outsideSafety']} | {value(row['faultDelay'])} | {value(row['mqttAvailability'])} | "
            f"{row['findings']} | {row['findingCodes']} |")
    lines.extend([
        "",
        "## Model Vs Heuristic Advisory Counts",
        "",
        "| Scenario | Production findings | Model findings | Heuristic findings |",
        "| --- | ---: | ---: | ---: |",
    ])
    for row in scenario_rows:
        lines.append(
            f"| `{row['scenario']}` | {row['findings']} | {row['modelFindings']} | {row['heuristicFindings']} |")
    lines.extend([
        "",
        "## Economic And Safety Evidence",
        "",
        "| Scenario | Recommendations | Estimated value USD | Safety-envelope blocked advisories |",
        "| --- | --- | ---: | ---: |",
    ])
    for row in scenario_rows:
        lines.append(
            f"| `{row['scenario']}` | {row['recommendations']} | {row['annualValue']} | {row['safetyBlocked']} |")
    lines.extend([
        "",
        "## Finding Counts",
        "",
        "| Finding code | Count |",
        "| --- | ---: |",
    ])
    for finding, count in sorted(finding_counts.items()):
        lines.append(f"| `{finding}` | {count} |")
    lines.extend([
        "",
        "## Evidence Files",
        "",
        "| File | Bytes | SHA-256 |",
        "| --- | ---: | --- |",
    ])
    for item in evidence:
        lines.append(f"| `{item['relativePath']}` | {item['bytes']} | `{item['sha256']}` |")

    output = output_dir / "production-run-report.md"
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return output


def value(item: Any) -> str:
    return "-" if item is None else str(item)


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model-dir", type=Path, default=DEFAULT_MODEL_DIR)
    parser.add_argument("--run-dir", type=Path, default=DEFAULT_RUN_DIR)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    training_report = write_training_report(args.model_dir, args.output_dir)
    production_report = write_production_report(args.run_dir, args.model_dir, args.output_dir)
    print(json.dumps({
        "trainingReport": str(training_report.resolve()),
        "productionReport": str(production_report.resolve()),
        "outputDir": str(args.output_dir.resolve()),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
