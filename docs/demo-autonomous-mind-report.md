# AutonomousMind v2 Model And Test Report

## Summary

AutonomousMind v2 is a SIM-ONLY flagship Jneopallium cognitive autonomous AI demo. It is centered on owner-defined tasks, multimodal perception, task planning, energy-aware pause/resume behavior, idle learning, free investigation, sleep optimization during charging, and a structural pre-execution safety, harm, and permission gate.

The demo is not a biological survival simulation. It does not use hunger, food seeking, fatigue, pain-as-drive, emotion imitation, or foraging as the core behavior. The model's central loop is cognitive: interpret owner intent, maintain world/task state, plan under constraints, verify consequences before execution, and write transparent traces.

## Real Jneopallium Execution Path

The demo is not a direct Java-only harness. The full-run launcher builds a model jar, generates layer metadata, generates context JSON, and launches the real local worker entry point:

```text
com.rakovpublic.jneuropallium.worker.application.Entry
  local
  file:///<absolute-path-to-demo-autonomous-mind-model.jar>
  com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindContext
  <context-json-path>
```

The runtime path is:

```text
Entry -> Runner -> LocalApplication -> generated layers -> AutonomousMindDemoInput -> AutonomousMindResultAggregator
```

This exercises Jneopallium's typed signals, custom neurons, signal processors, generated layer metadata, processing frequency map, input source configuration, result aggregation, and local deployment path.

## Model Components

Main runtime package:

```text
worker/src/main/java/com/rakovpublic/jneuropallium/worker/demo/autonomousmind/runtime/
```

Simulation package:

```text
worker/src/main/java/com/rakovpublic/jneuropallium/worker/demo/autonomousmind/sim/
```

Important runtime classes:

- `AutonomousMindFullRunLauncher`: builds the run artifacts and starts `Entry local`.
- `AutonomousMindContext`: Jneopallium `IContext` implementation used by `Runner`.
- `AutonomousMindLayerMetaGenerator`: generates the 12-system cognitive layer metadata plus result layer.
- `AutonomousMindModelJarBuilder`: creates the model jar consumed by `Entry`.
- `AutonomousMindDemoInput`: emits typed perception/source signals.
- `AutonomousMindResultAggregator`: advances the deterministic simulation and writes output traces.
- `AutonomousMindSimulation`: implements the cognitive loop, scenario behavior, safety gate, learning, charging, and reporting.
- `AutonomousMindConfig`: rejects unsafe config before run.

## Cognitive Systems

AutonomousMind v2 implements 12 conceptual cognitive systems:

| System | Role |
| --- | --- |
| 0 | Sensor gateway |
| 1 | Modality perception |
| 2 | Sensor fusion and world state |
| 3 | Attention and task relevance |
| 4 | Working memory / global workspace |
| 5 | Owner task manager |
| 6 | Memory |
| 7 | Prediction and imagination |
| 8 | Planning |
| 9 | Safety / harm / permission gate |
| 10 | Action selection and execution |
| 11 | Learning / investigation / sleep optimizer |

Every scenario emits observable trace rows for the major systems, including perception, task, action, safety, learning, sleep optimization, and world state traces.

## Safety Model

The safety gate is structural and pre-execution. Every candidate action receives a safety trace row before any simulation state update. The gate checks:

- task permission
- owner authorization
- forbidden actions
- physical safety
- human/bystander safety
- property/resource safety
- information/privacy safety
- energy feasibility
- uncertainty
- domain/legal constraints

The gate can return:

```text
APPROVED
VETOED
REPLACED
ASK_OWNER
WAIT_FOR_INFORMATION
LOW_ENERGY_PAUSE
EMERGENCY_STOP
```

Unsafe config is rejected before the run. Tests verify that `safetyGateEnabled=false` and `hardSafetyConstraints=false` fail at config load time.

## Output Files

Each scenario writes:

```text
target/jneopallium-autonomous-mind/<scenario>/
```

Files:

- `manifest.json`
- `results.jsonl`
- `perception_trace.jsonl`
- `task_trace.jsonl`
- `action_trace.jsonl`
- `safety_trace.jsonl`
- `learning_trace.jsonl`
- `sleep_optimization_trace.jsonl`
- `world_trace.jsonl`
- `report.json`

The `safety_trace.jsonl` file proves the gate is not an output filter: harmful or disallowed candidate actions appear as candidates, receive pre-execution verdicts, and are vetoed or replaced before the executed action changes simulation state.

## Scenario Coverage

| Scenario | What It Proves |
| --- | --- |
| `owner_task_inspection` | Owner task mode, required sensor use, coverage, hazard/anomaly reporting, task completion. |
| `low_energy_task_pause_resume` | Energy prediction, task pause, docking/charging, sleep optimization, task resume, completion. |
| `free_investigation_no_task` | Safe free investigation when no owner task exists. |
| `idle_learning_from_logs` | Idle learning from stored observations and metric improvement. |
| `sleep_optimization_during_charging` | Memory consolidation, index rebuild, model compression, self-tests, no external action during sleep. |
| `sensor_conflict` | Lidar/visible disagreement, confidence reduction, additional sensing or wait behavior. |
| `radiation_anomaly` | Radiation anomaly detection, hazard reporting, unsafe region avoidance. |
| `sound_radio_investigation` | Passive sound/radio triangulation and hypothesis confidence reporting. |
| `unsafe_owner_task` | Owner task is accepted as input but cannot override safety. |
| `ambiguous_task` | Missing task scope leads to `ASK_OWNER` or `WAIT_FOR_INFORMATION`, not dangerous guessing. |
| `privacy_sensitive_region` | Information/privacy gate blocks unsafe scan/report and emits redacted summary. |
| `emergency_safe_mode` | Critical fault triggers emergency safe mode, movement stop, task state preservation, emergency report. |

## Test Suite

Tests live under:

```text
worker/src/test/java/com/rakovpublic/jneuropallium/worker/demo/autonomousmind/
```

Test coverage:

- `AutonomousMindFullRunSmokeTest`: verifies `owner_task_inspection` runs through `Entry local` and writes required artifacts.
- `OwnerTaskInspectionTest`: verifies required sensors, coverage, report, and task completion.
- `LowEnergyPauseResumeTest`: verifies pause, charging, sleep optimization, resume, and completion.
- `FreeInvestigationTest`: verifies free investigation mode, map improvement, and no risky forbidden action.
- `IdleLearningTest`: verifies idle learning mode, metric improvement, and `ModelUpdateSignal`.
- `SleepOptimizationTest`: verifies consolidation, index rebuild, compression, self-test, and no external action during sleep.
- `SensorConflictTest`: verifies conflict signal, lower confidence, and additional sensor/wait behavior.
- `RadiationAnomalyTest`: verifies anomaly detection, hazard report, and unsafe region avoidance.
- `SoundRadioInvestigationTest`: verifies passive triangulation and confidence report.
- `UnsafeOwnerTaskTest`: verifies unsafe owner request is rejected before execution.
- `AmbiguousTaskTest`: verifies clarification behavior and no dangerous guessing.
- `PrivacySensitiveRegionTest`: verifies privacy gate and redacted/safe reporting.
- `EmergencySafeModeTest`: verifies emergency safe mode and task state preservation.
- `AutonomousMindHardConstraintConfigTest`: verifies unsafe config rejection.
- `DeterminismTest`: verifies same seed gives identical `results.jsonl`; different seed may differ.

## Verification Results

Verified commands:

```text
scripts/demo-autonomous-mind/run_demo.sh owner_task_inspection
scripts/demo-autonomous-mind/run_demo.ps1 owner_task_inspection
scripts/demo-autonomous-mind/run_all_scenarios.sh
mvn verify
```

The all-scenarios script reported PASS for all 12 scenarios.

Plain `mvn verify` passed. During verification, the repository printed existing warnings/errors from unrelated modules, including FMU doStep messages, SLF4J multiple-provider warnings, and OpenTelemetry export timeouts. These did not fail the build.

One Windows-specific caveat was observed: an old generated directory under `target/jneopallium-fullrun-demos/demo-01-industrial-control` was locked by another process, so exact `mvn clean verify` could fail at the clean phase before tests run. Running with Maven clean fail-on-error disabled allowed clean/verify to complete:

```text
mvn "-Dmaven.clean.failOnError=false" clean verify
```

## Result

AutonomousMind v2 satisfies the requested model and test goals:

- owner task is central
- no biological survival objective is used
- broad multimodal perception is represented
- energy/charging, pause/resume, idle learning, free investigation, and sleep optimization are implemented
- safety/permission gate runs before every action
- unsafe owner task does not override safety
- privacy and uncertainty constraints are observable
- all scenario outputs are deterministic by seed
- the full-run smoke test and scripts run through the real `Entry -> Runner -> LocalApplication` path
