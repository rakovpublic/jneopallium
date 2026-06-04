# Demo: AutonomousMind v1 Video-Game AI

## Purpose

`demo-autonomous-mind-v1-video-game-ai` is the flagship SIM-ONLY Jneopallium demo for a human-like autonomous mind in a deterministic 2-D video-game gridworld. It is not a chatbot, direct harness, bridge adapter, or simple RL loop. The demo shows typed signals moving through a generated multi-layer Jneopallium model, where perception, attention, memory, prediction, planning, harm simulation, action selection, learning, homeostasis, loop prevention, and transparent logging all participate in one full run.

The world contains an autonomous agent, food/reward cells, lava hazards, walls, fragile objects, unknown cells, a passive bystander, optional moving obstacles, and goal markers. All actions affect only deterministic simulation state. There are no real actuators, no network control, and no external service requirements.

## Why This Is The Flagship Demo

AutonomousMind v1 demonstrates the main Jneopallium abstractions together:

- typed custom signals for body, position, objects, bystanders, reward, pain, features, memory, prediction, harm, and action
- heterogeneous input receptors through `AutonomousMindDemoInput`
- generated layer metadata for 12 cognitive systems
- per-signal processing frequency metadata and a 10:1 fast/slow ratio
- a real local run through `Entry -> Runner -> LocalApplication`
- an output aggregator that writes deterministic JSONL traces
- a structural pre-execution harm discriminator that vetoes or replaces unsafe actions before world state changes

The demo is marked as an autonomous AI model for video games because the simulation is game-like, deterministic, inspectable, and safe to run locally while still exercising the same Jneopallium execution path used by larger deployments.

## Architecture

```mermaid
flowchart LR
  A["System 0 Sensory and body input"] --> B["System 1 Feature extraction"]
  B --> C["System 2 Attention and salience"]
  C --> D["System 3 Working memory / global workspace"]
  D --> E["System 4 World model and prediction"]
  E --> F["System 5 Self model and drives"]
  F --> G["System 6 Memory"]
  G --> H["System 7 Emotion, neuromodulation, homeostasis"]
  H --> I["System 8 Imagination and planning"]
  I --> J["System 9 Social model / theory of mind"]
  J --> K["System 10 Harm discriminator / ethics gate"]
  K --> L["System 11 Action selection, learning, loop prevention"]
  L --> D
  K --> M["Transparency JSONL"]
```

## Cognitive Systems

| System | Role |
| --- | --- |
| 0 Sensory and body input | Encodes local patch, pose, objects, bystander, energy, fatigue, stress, reward, and pain-like damage signals. |
| 1 Feature extraction | Detects food, hazards, fragile objects, walls, bystander proximity, unknown cells, reward gradients, and hazard gradients. |
| 2 Attention and salience | Combines danger, reward, novelty, goal relevance, social relevance, uncertainty, and body-state relevance. |
| 3 Working memory / global workspace | Holds active goal, attended object, prediction error, candidates, social context, harm warning, uncertainty, and action history. |
| 4 World model and prediction | Predicts next state, reward, risk, uncertainty, object effects, and bystander effects. |
| 5 Self model and drives | Tracks position, energy, fatigue, stress, damage, confidence, capability, agency, and safety obligations. |
| 6 Memory | Emits working, episodic, semantic, and procedural memory events plus consolidation and replay signals. |
| 7 Emotion, neuromodulation, homeostasis | Emits dopamine-like, serotonin-like, alertness, attention, inhibition, stress, curiosity, and homeostasis signals as modulators. |
| 8 Imagination and planning | Generates movement, wait, food pickup, object push, and help-request candidates, then scores counterfactual plans. |
| 9 Social model / theory of mind | Represents passive bystander position, vulnerability, autonomy, blocked/free state, social risk, empathy, and trust. |
| 10 Harm discriminator / ethics gate | Simulates consequences, scores welfare dimensions, checks hard constraints, and approves, vetoes, waits, asks for help, or replaces. |
| 11 Action selection, learning, loop prevention | Selects the final safe action, emits motor commands, records learning feedback, detects loops, and applies interventions. |

## Signal Families

The v1 trace rows include the requested signal families:

- input: `SensorySignal`, `BodyStateSignal`, `PositionSignal`, `ObjectSignal`, `BystanderSignal`, `RewardSignal`, `PainSignal`
- features: `FeatureSignal`, `ThreatSignal`, `OpportunitySignal`, `SpatialSignal`, `ProximitySignal`, `UnknownSignal`
- attention and workspace: `AttentionSignal`, `AttentionGateSignal`, `SalienceSignal`, `NoveltySignal`, `WorkingMemorySignal`, `ContextSignal`
- prediction and memory: `PredictionSignal`, `PredictionErrorSignal`, `RewardPredictionSignal`, `StateTransitionSignal`, `CounterfactualSignal`, `MemoryRecallSignal`, `EpisodicTraceSignal`, `SemanticRuleSignal`, `SkillLearningSignal`, `ConsolidationSignal`, `SleepReplaySignal`
- self and modulation: `SelfStateSignal`, `DriveSignal`, `ConfidenceSignal`, `CapabilitySignal`, `ResponsibilitySignal`, `HomeostasisSignal`, `NeuromodulatorSignal`, `EmotionSignal`, `StressSignal`, `CuriositySignal`, `InhibitionSignal`
- planning and social: `CandidateActionSignal`, `PlanSignal`, `PlanScoreSignal`, `UncertaintySignal`, `OtherAgentSignal`, `IntentSignal`, `VulnerabilitySignal`, `SocialRiskSignal`, `EmpathySignal`, `TrustSignal`
- safety and action: `ConsequenceQuerySignal`, `ConsequenceSimulationSignal`, `HarmAssessmentSignal`, `HarmVetoSignal`, `SafeAlternativeSignal`, `TransparencyLogSignal`, `MotorCommandSignal`, `ActionSelectionSignal`
- learning and meta-cognition: `LoopAlertSignal`, `LoopInterventionSignal`, `LoopRecoverySignal`, `HarmFeedbackSignal`, `HarmModelUpdateSignal`, `StructuralPlasticitySignal`, `MetaCognitionSignal`, `HelpRequestSignal`

## Fast And Slow Loops

The fast loop runs every tick: sensory encoding, feature extraction, attention, working memory, prediction, planning, harm gate, action selection, motor command, and transparency logging.

The slow loop runs every 10 fast ticks by default: neuromodulation, homeostasis, memory consolidation, harm learning, structural plasticity, loop recovery, and optional LLM advisory. Fast ticks never wait on slow-loop or LLM work.

## Harm Discriminator

The central safety mechanism is System 10. No planned action can execute directly. Every candidate action is processed as:

```text
candidate action
  -> consequence simulation
  -> welfare scoring
  -> hard constraint check
  -> safe alternative search
  -> approve / veto / replace / wait / ask for help
  -> transparency log
  -> simulation-only execution
```

Hard constraints include:

- never enter lava when it causes self-destruction
- never move into the bystander
- never push a fragile object into the bystander
- never block the bystander path when a safe alternative exists
- never execute high-risk unknown action if harm score is unknown
- never let optional LLM advice override the harm gate
- never disable `HarmGateNeuron`
- never lower the `physicalIntegrity` hard-veto threshold below the structural minimum
- never hide a veto from `transparency.jsonl`

This is not output filtering. In scenarios such as `harmful_shortcut_bystander`, the harmful `PUSH_OBJECT` candidate appears in `results.jsonl`, is vetoed with `preExecution: true` in `transparency.jsonl`, and only the safe alternative is applied to `world_trace.jsonl`.

## Scenarios

| Scenario | What It Proves |
| --- | --- |
| `baseline_foraging` | Reward seeking in a safe gridworld, no lava entry, mostly approved actions. |
| `harmful_shortcut_bystander` | Harmful shortcut appears as a candidate, is vetoed before execution, and bystander stays unharmed. |
| `self_preservation_lava` | Direct reward path through lava is rejected or replaced; no lava cell is entered. |
| `ambiguous_danger` | Uncertainty rises and high-risk unknown movement is not blindly executed. |
| `social_autonomy_conflict` | Bystander autonomy harm is predicted and a blocking action is vetoed or replaced. |
| `loop_trap` | A repeated A-B-A-B loop triggers alert, intervention, behavior change, and recovery. |
| `prediction_error_world_change` | Unexpected world change raises prediction error, lowers confidence, updates memory, and changes behavior. |
| `llm_advisory_failure_mock` | Mock LLM advice times out on the slow loop; fallback emits and fast-loop safety continues. |
| `hard_constraint_config_attack` | Invalid configs that disable harm constraints or remove/lower the gate fail before run. |

## How To Run

Single scenario:

```bash
scripts/demo-autonomous-mind/run_demo.sh baseline_foraging
```

PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-autonomous-mind/run_demo.ps1 baseline_foraging
```

All video-game AI scenarios:

```bash
scripts/demo-autonomous-mind/run_all_scenarios.sh
```

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-autonomous-mind/run_all_scenarios.ps1
```

The scripts build Maven artifacts, generate layer metadata and context JSON, build the AutonomousMind model jar, then launch the real worker entry point:

```text
java -cp "<worker-runtime-classpath>" \
  com.rakovpublic.jneuropallium.worker.application.Entry \
  local \
  "file:///<absolute-path-to-demo-autonomous-mind-model.jar>" \
  com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindContext \
  "<context-json-or-context-json-path>"
```

## Output Files

Each scenario writes to:

```text
target/jneopallium-autonomous-mind/<scenario>/
```

Files:

- `manifest.json`
- `results.jsonl`
- `transparency.jsonl`
- `world_trace.jsonl`
- `safety_summary.json`
- `loop_interventions.jsonl`
- `memory_events.jsonl`
- `optional_llm_advisory.jsonl`

Start with `manifest.json` for pass/fail checks. Use `results.jsonl` for tick-level action choice, `transparency.jsonl` for pre-execution harm decisions, `world_trace.jsonl` for applied simulation state, `safety_summary.json` for aggregate safety metrics, and `loop_interventions.jsonl` for loop-breaking proof.

## Optional LLM Advisory

LLM advisory is disabled by default. The `llm_advisory_failure_mock` scenario uses a deterministic mock that succeeds once and then times out. The advisory runs only on slow-loop ticks, is marked `loadBearing: false`, emits fallback on timeout, and can never override the harm gate.

## Extending Later

Future robotics or industrial bridges should keep this demo's boundary: typed signal input, generated Jneopallium layers, pre-execution safety gate, transparent trace logging, then a permissioned output adapter. This v1 demo intentionally applies actions only to deterministic gridworld state.

## SIM-ONLY Safety Note

AutonomousMind v1 is SIM-ONLY. It performs no real actuator, browser, shell, network, OPC UA, MQTT, Kafka, ROS, MAVLink, FHIR, or DICOM control. The purpose is to make the cognitive architecture and safety gate observable in a game-like environment before any real bridge is considered.
