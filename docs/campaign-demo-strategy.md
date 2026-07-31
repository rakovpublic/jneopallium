# Campaign demo strategy

## Decision rule

The matching engine persists the chain `prospect -> documented problem -> capability evidence ->
demo or DemoPlan -> proposition`. It chooses one of: use unchanged, configure, extend, design a
synthetic POC, research collaboration, integration proposition, investor proposition, or
disqualify. Every choice retains capability IDs and explicitly separates current implementation from
proposed work.

The configured mappings are:

| Prospect domain | Default demo | Disposition | Safety ceiling |
| --- | --- | --- | --- |
| Ad fraud / invalid traffic | `demo.adfraud` | Configure | Synthetic/shadow advisory |
| Industrial automation, twins, maintenance, process supervision | `demo.industrial_fmi` | Configure | Safety-gated simulation/advisory |
| Clinical workflow / health-IT | `demo.fullrun.04-clinical-fhir-advisory` | Configure | Synthetic clinician advisory |
| Medical imaging context | `demo.fullrun.05-dicom-readonly-context` | Use | Metadata-only, read-only |
| Cybersecurity triage | `demo.fullrun.06-cybersecurity-kafka-triage` | Configure | Analyst advisory; no automatic response |
| AIOps / observability | `demo.fullrun.07-observability-otel-export` | Use | Export/advisory |
| Robotics | `demo.uav_single` | Extend | Simulation only |
| UAV / swarm | `demo.uav_single` | Configure | Simulation only |
| Adaptive tutoring | `demo.fullrun.08-adaptive-tutoring-lti` | Use | Advisory learner support |
| Nengo interoperability | `demo.fullrun.09-nengo-interop` | Use | Research interoperability |
| No adequate existing demo | none | Design synthetic POC | Simulation/advisory/human review |

The capability registry also records AutonomousMind, autonomous-AI gridworld, industrial, full-run,
cluster, and other repository demonstrations, so new YAML rules can add them without changing the
engine.

## DemoPlan contract

When no demo is adequate, the stored plan includes the customer problem, scenario, why Jneopallium
may fit, reusable and new modules, bridges, synthetic-data plan, architecture, outputs, baseline,
success metrics, safety constraints, estimated hours, unresolved assumptions, and implementation
milestones. Default outputs are decision JSONL, evidence trail, baseline comparison, and limitations
report. Default verification covers deterministic replay, schema validity, evidence completeness,
and the false-positive/negative matrix for labeled synthetic cases.

Plans for clinical, medical, patient, UAV, BCI, security, critical-infrastructure, or other dangerous
domains are limited to simulation, read-only monitoring, advisory output, or safety validation.

## Auto-implementation gate

A new demo may be marked `safe_to_auto_implement` only when the prospect score meets the configured
threshold (72 by default), estimated work is within the configured 40-hour budget, synthetic/public
data is sufficient, no prohibited function is needed, and the scenario is reusable or strategically
important. The flag is an engineering queue decision, not an authorization to merge code or contact
the prospect.

The current campaign system intentionally does not synthesize or merge Java demo code. An approved
engineering job must create a separate branch such as `demo/<domain>-<scenario>`, restrict changes to
the new demo/fixtures/docs where possible, add deterministic tests, run the full Maven reactor, and
obtain review. Core runtime changes require a separate justification and review. This preserves the
task's requirement not to destabilize core Jneopallium modules.

## Demonstration quality bar

Before a demo can support an outreach claim, it needs:

1. A versioned input and output schema.
2. Fixed-seed synthetic or authorized public fixtures with provenance and licensing notes.
3. A simple rules baseline and, where meaningful, a conventional baseline.
4. Deterministic artifacts under `target/`, never generated source in runtime modules.
5. Tests covering success, ordinary negative cases, adversarial/edge cases, safety gates, and
   repeatability.
6. A run command for Windows and POSIX, expected outputs, resource budget, and cleanup behavior.
7. A model/demo card with intended use, prohibited use, limitations, missing data, and readiness.
8. No benchmark, regulatory, production, adoption, customer, or business-outcome claim unsupported
   by authoritative evidence.

## Promotion and reuse

Start with a synthetic replay and schema review. Promote to a customer-supplied de-identified replay
only under an approved data agreement. Shadow/advisory operation comes after calibration and review.
Autonomous action is a separate product, legal, safety, and operational decision and is never implied
by a demo.

After each legitimate evaluation, update the capability evidence, demo limitations, reusable module
list, baseline results, common questions, and domain score. A negative result is retained and may
reduce allocation; the optimizer never hides weak evidence to improve outreach volume.
