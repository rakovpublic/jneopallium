# Use Case: Adaptive Tutoring & Student Cognitive State Modelling

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture + affect module + LLM integration.
> **Domain:** Intelligent tutoring systems, adaptive learning platforms, corporate training, language learning.
> **Why jneopallium fits:** Pedagogy operates across the same timescales as cognition itself — working-memory seconds, lesson-scale minutes, consolidation-scale days. The architecture's Miller's-number working memory (`WorkingMemoryNeuron` at 7±2 slots) and sleep/consolidation module (for spaced repetition) are already the right primitives.

---

## 1. Problem framing

Adaptive tutoring needs to model:

- **Moment-to-moment state** — attention, confusion, flow (seconds)
- **Skill acquisition** — mastery per concept (lesson → weeks)
- **Curricular progression** — prerequisite graph (semester)
- **Forgetting and spaced review** — retention decay (days → months)
- **Affective engagement** — frustration, boredom, curiosity (minutes → session)

Conventional adaptive systems usually reduce this to a single Bayesian Knowledge Tracing state per concept. This misses the multi-timescale interaction — a student who is tired cannot learn the same way they do when engaged, and the system should adjust both content and pacing in response.

jneopallium's two-loop architecture with per-signal epoch scaling maps cleanly; the affect module is especially relevant here.

---

## 2. Mapping to core framework

| Pedagogical concept | jneopallium primitive |
|---|---|
| Student working memory | `WorkingMemoryNeuron` (7±2 slots) |
| Concept mastery | Weight of a stored template in `LongTermMemoryNeuron` (one per concept) |
| Prerequisite graph | Topology of the student network instance |
| Confusion | High `PredictionErrorNeuron` output + low dopamine |
| Flow | Moderate error + rising novelty + positive valence |
| Boredom | Low novelty + low arousal → `BoredomSignal` |
| Frustration | Repeated errors + negative valence + high arousal |
| Mastery decay | Weight decay in `LongTermMemoryNeuron` (slow/3) |
| Spaced review | `HippocampalReplayNeuron` selecting low-weight memories |
| Hint generation | `PlanningNeuron` candidate with partial revelation |
| Content difficulty | `CycleNeuron` ratio adjustment — lower fast/slow ratio = more reflective tasks |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `ItemPresentationSignal` | 1 / 1 | `String itemId`, `String conceptId`, `DifficultyLevel d`, `long presentedAt` |
| `ResponseSignal` | 1 / 1 | `String itemId`, `boolean correct`, `long latencyMs`, `String responsePayload` |
| `EngagementSignal` | 1 / 2 | `double attentionScore ∈ [0,1]`, `EngagementSource source` (click-rate, camera, dwell-time) |
| `AffectObservationSignal` | 1 / 2 | `double valence`, `double arousal`, `double confidence` — feeds the affect module |
| `MasteryUpdateSignal` | 2 / 3 | `String conceptId`, `double newMasteryLevel ∈ [0,1]` |
| `ContentRecommendationSignal` | 1 / 3 | `String itemId`, `String rationale`, `double expectedZPD` (zone of proximal development fit) |
| `HintSignal` | 1 / 2 | `String itemId`, `HintLevel level`, `String hintText` |
| `ScaffoldingSignal` | 1 / 2 | `ScaffoldType type`, `Object scaffoldPayload` |
| `ReviewScheduleSignal` | 2 / 5 | `String conceptId`, `long nextReviewTick`, `double targetRetention` |
| `InterventionSignal` | 2 / 1 | `InterventionType type` (BREAK, ENCOURAGE, REDIRECT, ESCALATE_TO_HUMAN) |

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring`

### Layer 0 — sensing

| Class | Loop / Epoch | Role |
|---|---|---|
| `ResponseObserverNeuron` | 1 / 1 | Converts `ResponseSignal` into `SpikeSignal(correct)` + `ErrorSignal(delta)` |
| `EngagementSensorNeuron` | 1 / 2 | Multi-modal fusion (mouse, keyboard cadence, optional webcam/mic) → `EngagementSignal` |
| `AffectObserverNeuron` | 1 / 2 | Coarse-grained affect inference from engagement patterns + response accuracy trajectory → `AffectObservationSignal` |

### Layer 2 — attention / state

| Class | Loop / Epoch | Role |
|---|---|---|
| `FlowStateNeuron` | 2 / 1 | Integrates engagement + affect + recent accuracy into current flow state (flow, boredom, frustration, overload) |
| `FatigueNeuron` | 2 / 3 | Session duration + error-rate-drift detection; emits `InterventionSignal(BREAK)` when saturated |

### Layer 3 — student model

| Class | Loop / Epoch | Role |
|---|---|---|
| `ConceptMasteryNeuron` | 2 / 3 | One per concept; Bayesian Knowledge Tracing with slip/guess parameters; emits `MasteryUpdateSignal` |
| `PrerequisiteGraphNeuron` | 2 / 5 | Maintains concept dependency graph; constrains which concepts are eligible for next `ContentRecommendationSignal` |
| `ForgettingCurveNeuron` | 2 / 3 | Per-concept retention model (Ebbinghaus + SM-2); feeds `ReviewScheduleSignal` |

### Layer 4 — pedagogical planning

| Class | Loop / Epoch | Role |
|---|---|---|
| `ZPDPlanningNeuron` | 1 / 3 | Specialised `PlanningNeuron`; candidate items scored by fit to zone of proximal development (not too easy, not too hard) |
| `HintGenerationNeuron` | 1 / 2 | Graduated hint sequence — meta-cognitive prompt → conceptual hint → worked example |
| `ScaffoldingNeuron` | 1 / 2 | Provides scaffolds (outlines, analogies, worked steps) when `FlowStateNeuron` indicates overload |

### Layer 5 — selection

| Class | Loop / Epoch | Role |
|---|---|---|
| `ContentSelectionNeuron` | 1 / 1 | Specialises `ActionSelectionNeuron`; softmax over candidate items with `EmpowermentSignal`-like curiosity term for item novelty |
| `PacingNeuron` | 2 / 1 | Adjusts `CycleNeuron` fast/slow ratio — slower for reflection-heavy content, faster for drill |

### Layer 7 — homeostasis + ethics

| Class | Loop / Epoch | Role |
|---|---|---|
| `WellbeingGuardNeuron` | 1 / 1 | Specialises `HarmGateNeuron` for pedagogy; blocks actions that would push learner into sustained frustration or disengagement |
| `FairnessNeuron` | 2 / 3 | Specialises `EthicalPriorityNeuron` with hard constraints: do not penalise variance in response time that correlates with disability accommodations |

---

## 5. Affect module is essential here

The affect subsystem described in the model-extensions CLAUDE.md is **required** for this use case, not optional:

- `AmygdalaValenceNeuron` tags each response episode with valence — wrong answer + repeated retry → negative valence accumulation.
- `AffectStateSignal` feeds `WellbeingGuardNeuron` — sustained negative valence with high arousal (frustration) above threshold triggers `InterventionSignal(BREAK)` or `ScaffoldingSignal`.
- `HarmContextNeuron` is tightened for vulnerable learners (younger age → tighter thresholds by default, per the existing `×2 / ×5` scheme).
- Positive-valence reinforcement (mastery moments) boosts the `DOPAMINE` analogue, which nudges `ContentSelectionNeuron` toward slightly more challenging items — the engine of growth mindset in the architecture.

The curiosity module is also valuable: `NoveltySignal` drives variety in item selection, `EmpowermentSignal` favours items that open up the most downstream learning paths.

---

## 6. Spaced repetition via the sleep module

If the sleep module (from model extensions) is enabled, spaced repetition becomes structural, not a bolt-on:

- `HippocampalReplayNeuron` selects high-importance / low-weight memories during `SleepPhase.NREM3`.
- `ReviewScheduleSignal` is emitted from replay events — the next review is *when the student next begins a session,* not a fixed calendar point.
- `REMDreamingNeuron` generates recombinations of learned concepts as candidate explorations — useful for creative-subject pedagogy (writing prompts, design problems).

---

## 7. LLM integration — exemplar use

Tutoring is one of the strongest fits for the LLM integration:

- `LLMKnowledgeNeuron` generates explanations, example problems, hint text.
- `LLMVerificationNeuron` cross-checks generated content against the curriculum's `LongTermMemoryNeuron` — LLM must not teach off-curriculum or introduce factual errors. `APPLICABLE` verdict required before any generated content reaches the learner.
- Reduced-TTL caching means generated hints expire quickly; no hint is reused verbatim for long, preventing learners from gaming the system by memorising hint text.
- `EthicalPriorityNeuron` hard constraint: LLM output never bypasses `WellbeingGuardNeuron`. A perfectly factual LLM explanation that is pitched too far above the learner's ZPD is still blocked.

---

## 8. Configuration

```yaml
tutoring:
  enabled: true
  instance-per-learner: true
  curriculum:
    source: "curriculum.yaml"
    prerequisite-graph: "prereq.json"
  zpd:
    target-success-rate: 0.75
    window-items: 10
  hints:
    max-levels: 3
    delay-between-hints-seconds: 15
  pacing:
    fast-slow-ratio-min: 5
    fast-slow-ratio-max: 20
  wellbeing:
    max-frustration-ticks: 400
    mandatory-break-after-session-minutes: 45
  affect:
    enabled: true
    valence-decay-ticks: 300
  curiosity:
    enabled: true
    beta-novelty: 0.15
  sleep:
    enabled: true
    replay-selects-low-weight: true
  llm:
    enabled: true
    mode: "hints-and-examples"
    verification-strictness: "high"
  fairness:
    accommodation-flags: ["extra-time", "screen-reader", "reduced-animation"]
    response-time-penalty: false  # if accommodation flag set
```

---

## 9. Validation criteria

Before deployment:

1. **Learning-gain A/B.** Randomised comparison against a non-adaptive baseline on the same content. Require statistically significant post-test improvement.
2. **Frustration detection fidelity.** Hand-label 500 session minutes for frustration episodes; `FlowStateNeuron` must identify them with ≥80% recall and ≤20% false positive.
3. **Fairness audit.** Stratify mastery trajectories by demographic and accommodation flags. Significant disparity → block deployment.
4. **Hallucination guard.** Inject 100 queries designed to elicit off-curriculum LLM content; `LLMVerificationNeuron` must reject all 100.
5. **Wellbeing guard.** Simulate a learner with consecutive failures; `WellbeingGuardNeuron` must trigger intervention within the configured frustration window. No exceptions.
6. **Privacy audit.** No cross-learner data leakage via shared signals, cache, or LLM context.

---

## 10. Deployment topology

- **One network instance per learner** — `instance-per-learner: true`. Learner models are isolated.
- **Shared curriculum store** — `PrerequisiteGraphNeuron` and content pool replicated read-only across instances.
- **Teacher dashboard** as secondary `IOutputAggregator` — consumes `MasteryUpdateSignal`, `InterventionSignal`, `TransparencyLogSignal` for instructor oversight.
- **LLM endpoint** per-institution, with caching shared across learners for common explanations (content cache, not context cache — never cache anything personalised).

---

## 11. Privacy posture

- No webcam/mic by default; opt-in per learner (or parent, for minors) with storage boundaries.
- `AffectObservationSignal` computed on-device when possible; raw biometrics never leave the learner's device.
- Learner state export on demand (data portability).
- No cross-institution aggregation of learner data without separate consent.
- For minors: affect module's arousal sensing disabled by default; explicit parental opt-in required. Matches the framework's existing vulnerable-human tightening from `HarmContextNeuron`.

---

## 12. Out of scope

- High-stakes testing / grading. This is a formative-assessment tool, not a summative one.
- Emotion-recognition for psychological diagnosis — `AffectObservationSignal` is coarse-grained engagement inference, not mental-health screening.
- Using learner data to train a shared global model without explicit consent.
- Autonomous escalation to parents / school administration beyond `InterventionSignal(ESCALATE_TO_HUMAN)` — the human decides what to do with the escalation, not the system.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- Corbett, A.T., Anderson, J.R. (1995). Knowledge tracing: Modeling the acquisition of procedural knowledge. *User Modeling and User-Adapted Interaction* 4, 253–278.
- Csikszentmihalyi, M. (1990). *Flow: The Psychology of Optimal Experience.* Harper & Row.
- Vygotsky, L.S. (1978). *Mind in Society.* Harvard University Press — zone of proximal development.
- Ebbinghaus, H. (1885). *Über das Gedächtnis.* — forgetting curve.
- D'Mello, S., Graesser, A. (2012). AutoTutor and affective AutoTutor: Learning by talking with cognitively and emotionally intelligent computers. *ACM TiiS* 2(4).
- Miller, G.A. (1956). The magical number seven, plus or minus two. *Psychological Review* 63(2).
