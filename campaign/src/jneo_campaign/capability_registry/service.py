from __future__ import annotations

import hashlib
import json
import subprocess
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Any

from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from jneo_campaign.storage.models import Capability, CapabilityEvidence

READINESS = {
    "demonstrated": "IMPLEMENTED_AND_DEMONSTRATED",
    "implemented": "IMPLEMENTED_NOT_DEMONSTRATED",
    "prototype": "PROTOTYPE",
    "design": "DESIGN_ONLY",
    "hypothesis": "RESEARCH_HYPOTHESIS",
    "unsupported": "NOT_SUPPORTED",
}


@dataclass(frozen=True)
class CapabilitySpec:
    capability_id: str
    name: str
    domain: str
    path_prefix: str
    kind: str
    demo_command: str | None = None
    docs: tuple[str, ...] = ()
    protocols: tuple[str, ...] = ()
    artifacts: tuple[str, ...] = ()
    limitations: tuple[str, ...] = ()
    safety: tuple[str, ...] = ()


DOMAIN_SPECS = [
    ("industrial", "Industrial multi-timescale supervision", "industrial"),
    ("security", "Adaptive cybersecurity triage", "cybersecurity"),
    ("adfraud", "Advertising-fraud signal processing", "ad-fraud"),
    ("swarm", "Multi-agent and swarm coordination signals", "swarm robotics"),
    ("clinical", "Advisory clinical workflow signal processing", "clinical"),
    ("bci", "BCI research and safety-gating primitives", "BCI research"),
    ("tutoring", "Adaptive tutoring signal processing", "education"),
    ("affect", "Human-machine affect modelling primitives", "affect research"),
    ("glia", "Glial modulation research primitives", "cognitive research"),
    ("sleep", "Sleep/consolidation research primitives", "cognitive research"),
    ("curiosity", "Curiosity-driven exploration primitives", "cognitive research"),
    ("embodiment", "Embodiment and body-schema primitives", "robotics research"),
    ("llm", "LLM advisory and verification orchestration", "AI orchestration"),
    ("agi", "Autonomous-agent research primitives", "autonomous AI research"),
]

BRIDGE_SPECS = [
    ("fmi", "FMI co-simulation bridge", "industrial digital twins", "FMI"),
    ("iec61850", "IEC 61850 bridge", "energy systems", "IEC 61850"),
    ("plc4x", "Apache PLC4X bridge", "industrial automation", "PLC4X"),
    ("canopen", "CANopen bridge", "industrial and vehicle systems", "CANopen"),
    ("mqtt", "MQTT and Sparkplug B bridge", "industrial IoT", "MQTT/Sparkplug B"),
    ("ditto", "Eclipse Ditto bridge", "digital twins", "Eclipse Ditto"),
    ("opcua", "OPC UA bridge", "industrial automation", "OPC UA"),
    ("kafka", "Apache Kafka bridge", "distributed event processing", "Kafka"),
    ("fhir", "FHIR advisory bridge", "health-IT interoperability", "FHIR"),
    ("dicom", "DICOM read-only context bridge", "medical imaging workflow", "DICOM"),
    ("lsl", "Lab Streaming Layer bridge", "BCI research", "LSL"),
    ("lti", "LTI and xAPI bridge", "education", "LTI/xAPI"),
    ("mavlink", "MAVLink advisory bridge", "UAV simulation", "MAVLink"),
    ("ros2", "ROS 2 bridge", "robotics", "ROS 2"),
    ("otel", "OpenTelemetry export bridge", "observability", "OpenTelemetry"),
    ("nengo", "Nengo interoperability bridge", "computational neuroscience", "Nengo"),
]

DEMO_SPECS = [
    CapabilitySpec(
        "demo.autonomousmind",
        "AutonomousMind deterministic gridworld demo",
        "autonomous AI research",
        "demos/demo-autonomousmind",
        "demo",
        "scripts/demo-autonomous-mind/run_demo.ps1 baseline_foraging",
        ("docs/demo-autonomous-mind.md", "docs/demo-autonomous-mind-report.md"),
        artifacts=(
            "target/jneopallium-autonomous-mind/<scenario>/manifest.json",
            "target/jneopallium-autonomous-mind/<scenario>/transparency.jsonl",
        ),
        limitations=(
            "Deterministic simulation only",
            "Mock LLM fallback is not evidence of general intelligence",
        ),
        safety=("Pre-execution harm veto", "No real-world actuation"),
    ),
    CapabilitySpec(
        "demo.autonomousai",
        "Autonomous AI gridworld demo",
        "autonomous AI research",
        "demos/demo-autonomousai",
        "demo",
        "scripts/demo-autonomous-ai/run_demo.ps1 baseline_foraging",
        ("docs/demo-autonomous-ai-gridworld.md",),
        artifacts=("target/jneopallium-autonomous-ai/<scenario>/manifest.json",),
        limitations=("Simulation only",),
        safety=("Harm discrimination and simulation boundary",),
    ),
    CapabilitySpec(
        "demo.industrial_fmi",
        "Industrial FMI Loop Guardian demo",
        "industrial automation",
        "demos/demo-industrialfmi",
        "demo",
        "scripts/demo-industrial-fmi/run_demo.ps1 all",
        ("docs/demo-industrial-fmi.md", "docs/demo-industrial-fmi-report.md"),
        ("FMI", "OPC UA", "MQTT"),
        (
            "target/jneopallium-industrial-fmi/<scenario>/manifest.json",
            "target/jneopallium-industrial-fmi/<scenario>/advisory-output.jsonl",
        ),
        ("Synthetic thermal skid; site validation is required",),
        ("Hard safety remains with PLC/SIS", "Bounded supervisory recommendations"),
    ),
    CapabilitySpec(
        "demo.uav_single",
        "Single-UAV observation and safety supervisor demo",
        "UAV simulation",
        "demos/demo-uavsingle",
        "demo",
        "scripts/demo-uav-single/run_demo.ps1 all",
        ("docs/demo-uav-single.md", "docs/demo-uav-single-report.md"),
        ("MAVLink-style telemetry",),
        ("target/jneopallium-uav-single/<scenario>/manifest.json",),
        ("Synthetic imagery and simulator movement only",),
        ("SIMULATOR_ONLY", "No command is sent to a flying vehicle"),
    ),
    CapabilitySpec(
        "demo.adfraud",
        "Synthetic advertising-fraud evaluation demo",
        "ad-fraud",
        "demos/demo-adfraud",
        "demo",
        "scripts/demo-ad-fraud/run_all.ps1",
        ("docs/modules/advertising-fraud.md",),
        ("OpenRTB-inspired synthetic events", "ads.txt", "sellers.json", "SupplyChain Object"),
        ("target/jneopallium-ad-fraud/summary.json", "target/jneopallium-ad-fraud/decisions.jsonl"),
        ("Synthetic data only", "No accreditation or guaranteed reduction claim"),
        ("Detection and simulation only; no offensive fraud tooling",),
    ),
    CapabilitySpec(
        "demo.cluster",
        "Redis-backed cluster runtime demo",
        "distributed execution",
        "demos/demo-cluster",
        "demo",
        "scripts/demo-cluster-redis/run_cluster_demo.ps1",
        ("docs/demo-cluster-redis.md",),
        artifacts=("target/jneopallium-cluster-demo/",),
        limitations=("Demo topology is not a production availability benchmark",),
    ),
]

FULLRUN = [
    (
        "01-industrial-control",
        "Industrial control safety-gating",
        "industrial automation",
        "AUTONOMOUS-MOCK",
    ),
    (
        "02-pump-fleet-maintenance",
        "Pump fleet predictive-maintenance advisory",
        "predictive maintenance",
        "ADVISORY",
    ),
    ("03-drone-mavlink-guard", "MAVLink mission-guard advisory", "UAV simulation", "SIM-ONLY"),
    (
        "04-clinical-fhir-advisory",
        "FHIR clinical advisory workflow",
        "health-IT interoperability",
        "ADVISORY",
    ),
    (
        "05-dicom-readonly-context",
        "DICOM read-only context workflow",
        "medical imaging workflow",
        "READ-ONLY",
    ),
    ("06-cybersecurity-kafka-triage", "Kafka cybersecurity triage", "cybersecurity", "ADVISORY"),
    ("07-observability-otel-export", "OpenTelemetry export workflow", "observability", "READ-ONLY"),
    ("08-adaptive-tutoring-lti", "LTI adaptive-tutoring workflow", "education", "ADVISORY"),
    (
        "09-nengo-interop",
        "Nengo interoperability workflow",
        "computational neuroscience",
        "RESEARCH",
    ),
]


def capability_specs() -> list[CapabilitySpec]:
    specs = [
        CapabilitySpec(
            "core.typed_multitimescale_runtime",
            "Typed, multi-timescale neural signal runtime",
            "core platform",
            "worker-core",
            "core",
            docs=("README.md",),
            limitations=(
                "Research framework; repository evidence does not establish production readiness",
            ),
        )
    ]
    for slug, name, domain in DOMAIN_SPECS:
        safety: tuple[str, ...] = ()
        limits: tuple[str, ...] = ("Requires domain-specific validation and integration",)
        if slug == "clinical":
            safety = (
                "Advisory only",
                "Human review required",
                "No diagnosis or treatment automation",
            )
        elif slug in {"bci", "swarm"}:
            safety = ("Research/simulation use unless separately validated",)
        specs.append(
            CapabilitySpec(
                f"domain.{slug}",
                name,
                domain,
                f"domains/domain-{slug}",
                "domain",
                docs=(f"docs/modules/{'advertising-fraud' if slug == 'adfraud' else slug}.md",),
                limitations=limits,
                safety=safety,
            )
        )
    for slug, name, domain, protocol in BRIDGE_SPECS:
        safety = ("Starts in a non-actuating or advisory safety mode",)
        if slug == "dicom":
            safety = ("Read-only context; no pixel diagnosis or writeback",)
        elif slug == "fhir":
            safety = ("Advisory output; clinician review required",)
        elif slug == "mavlink":
            safety = ("SIM-ONLY/advisory; forbidden flight commands enforced",)
        specs.append(
            CapabilitySpec(
                f"bridge.{slug}",
                name,
                domain,
                f"bridges/bridge-{slug}",
                "bridge",
                docs=(f"docs/{slug}-bridge.md",),
                protocols=(protocol,),
                limitations=(
                    "Customer-specific mapping, security review, and acceptance testing required",
                ),
                safety=safety,
            )
        )
    specs.extend(DEMO_SPECS)
    for slug, name, domain, mode in FULLRUN:
        specs.append(
            CapabilitySpec(
                f"demo.fullrun.{slug}",
                name,
                domain,
                "demos/demo-fullrun",
                "demo",
                f"scripts/demo-fullrun/run_demo.ps1 demo-{slug}",
                (f"docs/demo-fullrun/reports/demo-{slug}-report.md", "docs/demo-fullrun/README.md"),
                artifacts=(f"target/jneopallium-fullrun-demos/demo-{slug}/manifest.json",),
                limitations=(
                    "Deterministic application-style demo; not customer deployment evidence",
                ),
                safety=(mode,),
            )
        )
    return specs


class CapabilityRegistryBuilder:
    def __init__(self, repository_root: Path, report_dir: Path) -> None:
        self.repository_root = repository_root.resolve()
        self.report_dir = report_dir.resolve()

    def build(self, session: Session) -> dict[str, Any]:
        tracked = self._tracked_files()
        commit = self._git("rev-parse", "HEAD").strip()
        history = self._git("log", "-12", "--pretty=format:%h %ad %s", "--date=short").splitlines()
        records = [self._record(spec, tracked) for spec in capability_specs()]
        for record in records:
            self._persist(session, record)
        registry = {
            "schema_version": "1.0",
            "generated_at": datetime.now(UTC).isoformat(),
            # Keep the committed registry portable and avoid leaking a developer's local path.
            "repository_root": ".",
            "repository_commit": commit,
            "audited_roots": [
                "README.md",
                "pom.xml",
                "domains/",
                "bridges/",
                "demos/",
                "scripts/",
                "docs/",
                "doc/",
                "integration-tests/",
                "WorkDiary.md",
                "git history",
            ],
            "tracked_file_count": len(tracked),
            "history_sample": history,
            "capabilities": records,
        }
        self._write_reports(registry)
        return registry

    def _tracked_files(self) -> list[str]:
        output = self._git("ls-files", "-z")
        return sorted(
            path for path in output.split("\0") if path and not path.startswith("campaign/")
        )

    def _git(self, *args: str) -> str:
        result = subprocess.run(  # noqa: S603 - fixed executable and internal argument set
            ["git", *args],  # noqa: S607 - resolved from the operator's normal PATH
            cwd=self.repository_root,
            check=True,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        return result.stdout

    def _record(self, spec: CapabilitySpec, tracked: list[str]) -> dict[str, Any]:
        relevant = [
            path
            for path in tracked
            if path == spec.path_prefix or path.startswith(spec.path_prefix + "/")
        ]
        source = [path for path in relevant if "/src/main/" in path]
        tests = [path for path in relevant if "/src/test/" in path or "/tests/" in path]
        docs = [path for path in spec.docs if path in tracked]
        scripts = [
            path
            for path in tracked
            if path.startswith("scripts/")
            and spec.capability_id.split(".")[-1].replace("_", "-") in path.lower()
        ]
        if spec.kind == "demo" and source and (tests or scripts):
            readiness = READINESS["demonstrated"]
            status = "Source, runnable command, and repository verification evidence are present"
        elif source and tests:
            readiness = READINESS["implemented"]
            status = "Source and tests are present; no customer deployment is evidenced"
        elif source:
            readiness = READINESS["prototype"]
            status = "Source is present, but demonstrated verification evidence is incomplete"
        elif docs:
            readiness = READINESS["design"]
            status = "Documentation exists without implementation evidence in the mapped module"
        else:
            readiness = READINESS["unsupported"]
            status = "No mapped implementation evidence was found"
        evidence_paths = sorted(set(source + tests + docs + scripts))
        digest = hashlib.sha256("\n".join(evidence_paths).encode()).hexdigest()
        if readiness == READINESS["demonstrated"]:
            claim = f"Jneopallium currently demonstrates {spec.name} in a repository-backed deterministic demo."
        elif readiness == READINESS["implemented"]:
            claim = f"The repository contains an implementation of {spec.name}; customer-specific validation is required."
        elif readiness == READINESS["prototype"]:
            claim = f"The repository contains a prototype of {spec.name}; it is not production evidence."
        else:
            claim = f"A proposed proof of concept would evaluate {spec.name}; it is not a current capability claim."
        prohibited = [
            "Production readiness",
            "Guaranteed performance or outcome",
            "Customer adoption",
            "Revenue or investment",
            "Certification or accreditation",
            "Regulatory approval",
            "Clinical effectiveness",
            "Military deployment",
            "Unverified benchmark results",
        ]
        return {
            "capability_id": spec.capability_id,
            "domain": spec.domain,
            "name": spec.name,
            "implementation_status": status,
            "source_files": source,
            "documentation": docs,
            "runnable_demo_command": spec.demo_command,
            "test_evidence": tests,
            "generated_artifacts": list(spec.artifacts),
            "supported_protocol_or_bridge": list(spec.protocols),
            "limitations": list(spec.limitations),
            "safety_constraints": list(spec.safety),
            "readiness": readiness,
            "allowed_marketing_claims": [claim],
            "prohibited_or_unsupported_claims": prohibited,
            "evidence_digest": digest,
        }

    def _persist(self, session: Session, record: dict[str, Any]) -> None:
        item = session.scalar(
            select(Capability).where(Capability.capability_id == record["capability_id"])
        )
        if item is None:
            item = Capability(capability_id=record["capability_id"])
            session.add(item)
        item.domain = record["domain"]
        item.name = record["name"]
        item.implementation_status = record["implementation_status"]
        item.source_files = record["source_files"]
        item.documentation = record["documentation"]
        item.runnable_demo_command = record["runnable_demo_command"]
        item.test_evidence = record["test_evidence"]
        item.generated_artifacts = record["generated_artifacts"]
        item.protocols = record["supported_protocol_or_bridge"]
        item.limitations = record["limitations"]
        item.safety_constraints = record["safety_constraints"]
        item.readiness = record["readiness"]
        item.allowed_claims = record["allowed_marketing_claims"]
        item.prohibited_claims = record["prohibited_or_unsupported_claims"]
        item.evidence_digest = record["evidence_digest"]
        session.flush()
        session.execute(
            delete(CapabilityEvidence).where(CapabilityEvidence.capability_id == item.id)
        )
        for evidence_type, paths in (
            ("source", item.source_files),
            ("documentation", item.documentation),
            ("test", item.test_evidence),
        ):
            for path in paths:
                digest = hashlib.sha256(path.encode()).hexdigest()
                session.add(
                    CapabilityEvidence(
                        capability_id=item.id,
                        evidence_type=evidence_type,
                        path=path,
                        excerpt=f"Tracked repository {evidence_type} evidence: {path}",
                        sha256=digest,
                    )
                )

    def _write_reports(self, registry: dict[str, Any]) -> None:
        self.report_dir.mkdir(parents=True, exist_ok=True)
        (self.report_dir / "capability-registry.json").write_text(
            json.dumps(registry, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
        )
        lines = [
            "# Jneopallium capability registry",
            "",
            f"Repository commit: `{registry['repository_commit']}`",
            "",
            f"Tracked files audited: {registry['tracked_file_count']}",
            "",
            "| Capability | Domain | Readiness | Evidence |",
            "|---|---|---|---:|",
        ]
        for item in registry["capabilities"]:
            count = (
                len(item["source_files"]) + len(item["documentation"]) + len(item["test_evidence"])
            )
            lines.append(
                f"| `{item['capability_id']}` | {item['domain']} | {item['readiness']} | {count} |"
            )
        lines.extend(
            [
                "",
                "## Claim policy",
                "",
                "Generated propositions may use only the allowed claim attached to a capability record. "
                "A demonstrated repository artifact is not evidence of production deployment, certification, "
                "customer adoption, benchmark superiority, or domain effectiveness.",
                "",
            ]
        )
        (self.report_dir / "capability-registry.md").write_text("\n".join(lines), encoding="utf-8")
