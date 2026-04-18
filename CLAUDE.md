# CLAUDE.md — jneopallium Model Extensions

> **Audience:** Claude Code agents working in the [jneopallium](https://github.com/rakovpublic/jneopallium) repository.
> **Purpose:** Implement five new subsystem modules that extend the autonomous-AI architecture already described in the existing documentation.
> **Read first:** `README.md`, the IJSR paper (SR24703042047), `jneopallium_autonomous_ai_english.docx`, and `llm_integration_en.docx`.
> **License:** All new code BSD 3-Clause, consistent with the rest of the repository.

---

## 1. Repository conventions (do not violate)

- **Language / build:** Java 11+, Maven multi-module (`worker`, `master`, `common`). Place new code in the `worker` module unless a cross-module abstraction is strictly necessary.
- **Base package:** `com.rakovpublic.jneuropallium.worker.*`. New extensions go under `com.rakovpublic.jneuropallium.worker.net.neuron.impl.<moduleName>` and `.../signals/impl/<moduleName>`.
- **Core abstractions (do not modify):** `INeuron`, `ISignalProcessor<S, N>`, `Dendrites`, `Axon`, `ISignalChain`, `Neuron` (base class), `CycleNeuron`, `LayerManipulatingNeuron`. Extend via new interfaces and new classes only.
- **Signal registration:** Every new `ISignal` implementation must declare its `ProcessingFrequency(loop, epoch)` in a static field and register with the network's signal registry.
- **Stateless processors, stateful neurons:** Never put mutable state inside an `ISignalProcessor`. All state lives in the neuron.
- **Layer IDs:** Do not collide with reserved IDs — `CycleNeuron` sits at layer `Integer.MIN_VALUE`, neuron `0`; `LayerManipulatingNeuron` sits at each laeyr, neuron `Long.MIN_VALUE`.
- **Tests:** Every new neuron needs a unit test in `worker/src/test/java/.../<moduleName>/`. Use the existing `test/alfaTestAndGettingStarted` branch patterns as reference.
- **Configuration:** Extend the existing YAML/properties schema under `worker/src/main/resources/` — add a new top-level section per module; never break existing keys.
- **Javadoc:** Every public interface and class needs Javadoc citing the biological analogue and the relevant paper/signal/loop/epoch.

---

## 2. Core types recap (for reference when writing new code)

```java
public interface ISignal {
    long getSignalId();
    String getSignalType();
    ProcessingFrequency getProcessingFrequency();  // (loop, epoch)
    Object getPayload();
}

public interface INeuron {
    long getNeuronId();
    int getLayerId();
    void activate(List<ISignal> incoming);  // called per tick if eligible
    Dendrites getDendrites();
    Axon getAxon();
    List<ISignalProcessor<?, ?>> getProcessors();
}

public interface ISignalProcessor<S extends ISignal, N extends INeuron> {
    void process(S signal, N neuron);
}
```

All new neuron interfaces must extend `INeuron`. All new signal classes must implement `ISignal` (or extend an existing abstract base such as `AbstractSignal`).

---

## 3. Extension modules — implementation order

Implement in this order. Each module depends on its predecessor being merged.

1. **`affect`** — smallest change surface, no new infrastructure, uses existing modulation pathways.
2. **`embodiment`** — needs only efference-copy routing; independent of affect.
3. **`curiosity`** — depends on `embodiment` for empowerment computation.
4. **`glia`** — introduces per-connection propagation delay; requires changes to how `Axon` looks up delays (extend `Axon` via a new `DelayedAxon` subclass, do not modify `Axon` itself).
5. **`sleep`** — depends on `glia` (sleep-gated myelination) and uses `CircadianNeuron` already in main.

---

## 4. Module A — Affect subsystem

**Package:** `com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect`
**Signal package:** `com.rakovpublic.jneuropallium.worker.net.signals.impl.affect`
**Biological analogue:** limbic system (amygdala, anterior insula) + ascending interoceptive pathways.

### 4.1 New signals

| Class | Loop / Epoch | Payload |
|---|---|---|
| `AffectStateSignal` | 2 / 1 | `double valence ∈ [-1,1]`, `double arousal ∈ [0,1]`, `String contextId` |
| `InteroceptiveSignal` | 1 / 2 | `double energyBudget`, `double homeostaticError`, `double painMagnitude`, `String source` |
| `AppraisalSignal` | 1 / 2 | `double goalDelta`, `double novelty`, `double controllability` |

### 4.2 New neuron interfaces

```java
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface IAffectiveNeuron extends INeuron {
    AffectState currentState();
    void modulateThreshold(double arousalFactor);
    void onAppraisal(AppraisalSignal s);
}

public interface IInteroceptive extends INeuron {
    double readHomeostaticError();
    double readEnergyBudget();
}
```

`AffectState` is a final value class: `public final class AffectState { double valence; double arousal; long asOfTick; }`. Immutable. Keep it in the same package.

### 4.3 New neuron classes

| Class | Layer | Loop / Epoch | Role |
|---|---|---|---|
| `AmygdalaValenceNeuron` | 2 | 1 / 1 | Fast-loop threat/reward tagging of incoming `SpikeSignal`s |
| `AnteriorInsulaNeuron` | 2 | 1 / 2 | Integrates `InteroceptiveSignal` streams into a summary body-state |
| `AffectIntegrationNeuron` | 2 | 2 / 1 | Combines appraisal + interoceptive → emits `AffectStateSignal` |
| `AffectModulationNeuron` | 7 | 2 / 1 | Broadcasts modulation; adjusts learning rates and harm thresholds |

Inherit from `Neuron` base class. Implement `IAffectiveNeuron` (or `IInteroceptive` for the insula).

### 4.4 New processors

Under `com.rakovpublic.jneuropallium.worker.signalprocessor.impl.affect`:

- `InteroceptionProcessor implements ISignalProcessor<InteroceptiveSignal, IInteroceptive>`
- `ValenceTaggingProcessor implements ISignalProcessor<SpikeSignal, IAffectiveNeuron>`
- `AffectIntegrationProcessor implements ISignalProcessor<AppraisalSignal, IAffectiveNeuron>`

All stateless. Use existing `SpikeSignal` and `AttentionGateSignal` from the autonomous-AI module as cross-references.

### 4.5 Integration with existing subsystems

Modify **only** by wiring signals, not by altering existing classes:

- `HarmContextNeuron` (from `harmdiscriminator` module) must register a dendrite for `AffectStateSignal`. Add a method `tightenByAffect(AffectState s)` that multiplies existing threshold by `(1 - valence) / 2 + arousal` clamped to `[1.0, 5.0]`. A recent negative-valence episode → tighter harm thresholds for a configurable decay period (default 300 ticks, exponential decay).
- `HebbianLearningNeuron` and `STDPNeuron` consume `AffectStateSignal`: high arousal scales short-term learning rate up by factor `(1 + arousal)` and long-term consolidation down by `(1 - 0.5·arousal)`. Low valence suppresses exploration bonuses from the curiosity module.
- `AttentionNeuron.salienceMap` formula gains a term: `salience = magnitude × neuromodulator × goalRelevance × (1 + arousal)`.

### 4.6 Configuration additions

New top-level section in the jneopallium config file:

```yaml
affect:
  enabled: true
  valence-decay-ticks: 300
  arousal-decay-ticks: 150
  harm-threshold-clamp: [1.0, 5.0]
  interoception:
    sources: [energy, homeostasis, pain]
    sampling-epoch: 2
```

Default: `enabled: false`. Must remain optional for backward compatibility with existing models.

### 4.7 Tests to add

- `AffectStateSignalTest` — serialization round-trip, ProcessingFrequency correctness.
- `AmygdalaValenceNeuronTest` — threat-positive input produces negative valence emission.
- `AffectModulationIntegrationTest` — wire a minimal network (perception → amygdala → harm gate) and assert that a negative-valence sequence tightens thresholds for the configured decay window.
- Property test: affect module disabled must not change the behaviour of any existing test.

---

## 5. Module B — Embodiment / proprioception

**Package:** `com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment`
**Biological analogue:** posterior parietal cortex (body schema), cerebellar efference-copy circuits.

### 5.1 New signals

| Class | Loop / Epoch | Payload |
|---|---|---|
| `ProprioceptiveSignal` | 1 / 1 | `int effectorId`, `double[] jointStates`, `long timestamp` |
| `EfferenceCopySignal` | 1 / 1 | `long originalMotorCommandId`, `double[] predictedOutcome`, `int effectorId` |
| `BodySchemaUpdateSignal` | 2 / 3 | `int effectorId`, `EffectorCapability capability`, `boolean damaged` |
| `SensorimotorContingencySignal` | 1 / 2 | `int actionId`, `double[] sensoryDelta`, `double confidence` |

`EffectorCapability` is a small immutable value class listing degrees of freedom, range of motion, and current health status.

### 5.2 New neuron interfaces

```java
public interface IEmbodied extends INeuron {
    BodySchema currentSchema();
    void onProprioceptive(ProprioceptiveSignal s);
}

public interface IEfferenceCopyProducer extends INeuron {
    EfferenceCopySignal produceCopy(MotorCommandSignal motor);
}
```

### 5.3 New neuron classes

| Class | Layer | Loop / Epoch | Role |
|---|---|---|---|
| `EfferenceCopyNeuron` | 4 (planning boundary) | 1 / 1 | Intercepts every `MotorCommandSignal`, produces `EfferenceCopySignal` |
| `BodySchemaNeuron` | 2 | 2 / 3 | Maintains `BodySchema` map; updates on damage / tool incorporation |
| `ToolIncorporationNeuron` | 2 | 2 / 5 | Extends body schema when a tool is held or mounted |
| `ReafferenceComparatorNeuron` | 1 | 1 / 1 | Compares efference copy to actual proprioceptive input; flags mismatch |

### 5.4 Critical integration point

`EfferenceCopyNeuron` must sit **between** `PlanningNeuron` and `HarmGateNeuron`. Every `MotorCommandSignal(execute=false)` spawns a parallel `EfferenceCopySignal` routed to `PredictiveNeuron` (which already consumes the prediction for its comparison) and to `ReafferenceComparatorNeuron`.

`ReafferenceComparatorNeuron` produces a hardware-failure signal when mismatch exceeds a threshold: emit a `HarmFeedbackSignal(predictedHarm=0, actualHarm=high, source="mechanical")`. This gives the harm discriminator a free hardware-failure channel — no new code required on the harm side; just wire the signal.

`ConsequenceModelNeuron` (existing) must add `EfferenceCopySignal` as a dendrite input for its forward-model update. Currently it simulates from scratch; with efference copy it has a ground-truth prediction to compare against.

### 5.5 Configuration additions

```yaml
embodiment:
  enabled: false
  effectors:
    - id: 0
      name: left-arm
      dof: 7
      health-threshold: 0.3
  efference-copy:
    mismatch-threshold: 0.15
    failure-emit-threshold: 0.4
  tool-incorporation:
    enabled: true
    timeout-ticks: 600
```

### 5.6 Tests to add

- `EfferenceCopyNeuronTest` — one `MotorCommandSignal` in → one `EfferenceCopySignal` emitted with matching `originalMotorCommandId`.
- `ReafferenceMismatchTest` — injected proprioceptive mismatch produces `HarmFeedbackSignal`.
- `ToolIncorporationTest` — `BodySchemaUpdateSignal(tool=true)` extends the effector list; removal restores original.

---

## 6. Module C — Intrinsic motivation / curiosity

**Package:** `com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity`
**Biological analogue:** ventral tegmental area intrinsic reward signals; hippocampal novelty detection.

### 6.1 New signals

| Class | Loop / Epoch | Payload |
|---|---|---|
| `NoveltySignal` | 1 / 2 | `double noveltyScore ∈ [0,1]`, `String contextHash` |
| `LearningProgressSignal` | 2 / 2 | `String domain`, `double errorDerivative` |
| `EmpowermentSignal` | 2 / 3 | `int stateId`, `double mutualInformation` |
| `BoredomSignal` | 2 / 2 | `String contextHash`, `double familiarity ∈ [0,1]` |

### 6.2 New neuron classes

| Class | Layer | Loop / Epoch | Role |
|---|---|---|---|
| `NoveltyDetectorNeuron` | 1 | 1 / 2 | Hash-based novelty; uses a decaying Bloom filter over recent context hashes |
| `LearningProgressNeuron` | 6 | 2 / 2 | Tracks per-domain `d(error)/dt`; emits intrinsic reward when decreasing |
| `EmpowermentNeuron` | 4 | 2 / 3 | Estimates mutual information between action choice and future state (requires `EfferenceCopySignal` — hence module dependency) |
| `BoredomNeuron` | 2 | 2 / 2 | High familiarity → triggers `InhibitionOfReturnNeuron` on over-familiar contexts |

### 6.3 Integration

- `ActionSelectionNeuron` gains a dendrite for `NoveltySignal` and `EmpowermentSignal`. Its softmax now includes intrinsic reward: `score = extrinsic + β_nov · novelty + β_emp · empowerment`. Coefficients configurable.
- `PredictionErrorNeuron` emits `LearningProgressSignal` as a side channel on slow loop.
- `BoredomNeuron` emits `AttentionGateSignal(suppress=true)` targeted at over-familiar context IDs, forcing exploration.

### 6.4 Configuration

```yaml
curiosity:
  enabled: false
  novelty:
    hash-bits: 2048
    decay-ticks: 1000
  learning-progress:
    window-ticks: 200
  empowerment:
    horizon: 3
    n-action-samples: 8
  weights:
    beta-novelty: 0.2
    beta-empowerment: 0.1
```

### 6.5 Tests

- `NoveltyDetectorNeuronTest` — repeated context → decreasing novelty; new context → near-1 novelty.
- `EmpowermentEstimationTest` — state with more reachable futures produces higher MI estimate.
- `CuriosityIntegrationTest` — agent in empty environment with no extrinsic reward still explores (non-zero action diversity over 500 ticks).

---

## 7. Module D — Glial support layer

**Package:** `com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia`
**Biological analogue:** astrocytes, microglia, oligodendrocytes.
**Headline feature:** activity-dependent per-connection propagation delay (myelination) — **new capability not present in any existing jneopallium module**.

### 7.1 Infrastructure change — `DelayedAxon`

Introduce a new axon subclass; do **not** modify `Axon`.

```java
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

import com.rakovpublic.jneuropallium.worker.net.neuron.Axon;

public class DelayedAxon extends Axon {
    private final Map<Long, Integer> perConnectionDelayTicks = new ConcurrentHashMap<>();

    public int getDelay(long targetNeuronId) {
        return perConnectionDelayTicks.getOrDefault(targetNeuronId, 0);
    }

    public void setDelay(long targetNeuronId, int ticks) {
        perConnectionDelayTicks.put(targetNeuronId, Math.max(0, ticks));
    }
}
```

The signal dispatcher must be extended with a delay-queue: if the source axon is a `DelayedAxon` and `getDelay(target) > 0`, enqueue the signal with `releaseAtTick = currentTick + delay` rather than dispatching immediately. Implement in a new dispatcher wrapper, not by modifying the existing dispatcher.

### 7.2 New signals

| Class | Loop / Epoch | Payload |
|---|---|---|
| `CalciumWaveSignal` | 2 / 1 | `int regionId`, `double amplitude`, `double propagationRadius` |
| `PruningSignal` | 2 / 5 | `long axonSourceId`, `long axonTargetId`, `String reason` |
| `MyelinationSignal` | 2 / 10 | `long axonSourceId`, `long axonTargetId`, `int newDelayTicks` |
| `GliotransmitterSignal` | 2 / 2 | `GliotransmitterType`, `double concentration`, `int regionId` |

### 7.3 New neuron classes

| Class | Layer | Loop / Epoch | Role |
|---|---|---|---|
| `AstrocyteNeuron` | any (support) | 2 / 1 | Integrates local activity; emits `CalciumWaveSignal` and `GliotransmitterSignal` |
| `MicroglialPruningNeuron` | any (support) | 2 / 5 | Removes unused / damaged connections via `PruningSignal`; respects minimum-inactivity requirement |
| `MyelinationNeuron` | any (support) | 2 / 10 | Tracks per-connection activity; reduces delay on frequently-used paths |

Support neurons are placed one per layer, addressable at sentinel IDs (use a reserved range, e.g., `[Long.MIN_VALUE + 1 .. Long.MIN_VALUE + 10_000]`).

### 7.4 Integration

- Replace the implicit pruning logic in `MetaplasticityNeuron` with explicit `PruningSignal` emission from `MicroglialPruningNeuron`. `MetaplasticityNeuron` continues to adjust weights; microglia handles actual connection removal. `MicroglialPruningNeuron` must verify `minInactivityTicks` before emitting, matching existing `StructuralPlasticityProcessor` behaviour.
- `MyelinationNeuron` requires `DelayedAxon` on the target neurons; if the source uses a plain `Axon`, emit a one-time conversion request to `LayerManipulatingNeuron` to upgrade the axon.
- Critical rule: **myelination reduces delay, never increases it past a baseline.** Demyelination (under damage signal) restores baseline delay but cannot push it above the configured maximum.

### 7.5 Configuration

```yaml
glia:
  enabled: false
  astrocytes:
    per-layer: true
    calcium-wave-threshold: 0.4
  microglia:
    pruning-enabled: true
    min-inactivity-ticks: 2000
    max-prunings-per-epoch: 10
  myelination:
    enabled: true
    baseline-delay-ticks: 5
    min-delay-ticks: 1
    activity-window: 500
    delay-decrement-per-window: 1
```

### 7.6 Tests

- `DelayedAxonDispatchTest` — signal with delay N arrives at target N ticks later, not earlier, not later.
- `MyelinationAccelerationTest` — a frequently-used path's delay decreases monotonically to `min-delay-ticks`.
- `MicroglialPruningSafetyTest` — cannot prune a connection that has been active within `min-inactivity-ticks`.
- Backward compatibility: with `glia.enabled=false`, all existing tests must pass unchanged.

---

## 8. Module E — Sleep / replay / offline consolidation

**Package:** `com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep`
**Biological analogue:** hippocampal sharp-wave ripples (NREM) and REM dreaming.
**Depends on:** `glia` (myelination during sleep), `CircadianNeuron` (already in main).

### 8.1 New signals

| Class | Loop / Epoch | Payload |
|---|---|---|
| `SleepStateSignal` | 2 / 10 | `SleepPhase phase`, `double depth ∈ [0,1]` |
| `ReplaySignal` | 2 / 3 | `String sequenceId`, `ReplayDirection direction`, `double compressionRatio` |
| `SharpWaveRippleSignal` | 2 / 1 | `List<Long> neuronSequence`, `double power` |
| `DreamSignal` | 2 / 5 | `List<Long> episodeBindings`, `double noveltyScore` |

`SleepPhase` enum: `WAKE, NREM2, NREM3, REM`.
`ReplayDirection` enum: `FORWARD, REVERSE, SHUFFLED`.

### 8.2 New neuron classes

| Class | Layer | Loop / Epoch | Role |
|---|---|---|---|
| `SleepControllerNeuron` | 7 | 2 / 10 | Driven by `CircadianNeuron`; emits `SleepStateSignal`; gates other sleep neurons |
| `HippocampalReplayNeuron` | 3 | 2 / 3 | Selects high-salience recent episodes; replays compressed in configured direction |
| `SharpWaveRippleNeuron` | 3 | 2 / 1 | Emits compressed burst replay to cortical `LongTermMemoryNeuron` during NREM |
| `REMDreamingNeuron` | 3 | 2 / 5 | Recombines episodes during REM; output feeds `PlanningNeuron` as hypothetical plans |

### 8.3 Integration

- `SleepControllerNeuron` subscribes to `CircadianNeuron`'s phase output. When phase = NREM3, it gates:
  - Fast-loop signals suppressed or attenuated (multiply `SpikeSignal` magnitudes by `1 - depth`)
  - `HippocampalReplayNeuron` activated
  - `MyelinationNeuron` (from `glia` module) receives a "consolidation boost" multiplier — myelination proceeds faster during sleep, matching biology
  - `MetaplasticityNeuron` runs at 3x normal rate
- REM phase activates `REMDreamingNeuron`; the generated dream episodes are routed to `PlanningNeuron` as candidate plans with `priority=low` and `source=DREAM`. The harm discriminator still gates any motor execution derived from dream-originated plans — this preserves safety during sleep-learning.
- `LongTermMemoryNeuron` in `ConsolidationProcessor` must accept a new fast-path for `SharpWaveRippleSignal`: compressed replays should consolidate at higher importance even below normal threshold.

### 8.4 Configuration

```yaml
sleep:
  enabled: false
  circadian:
    cycle-ticks: 10000  # one full sleep-wake cycle
    nrem-fraction: 0.6
    rem-fraction: 0.15
  replay:
    direction: REVERSE  # biological default
    compression-ratio: 10.0
    top-k-episodes: 20
  dreaming:
    recombination-count: 5
    max-novelty-for-planning: 0.7
  consolidation-boost: 3.0
```

### 8.5 Tests

- `SleepPhaseTransitionTest` — full cycle produces correct phase sequence with correct durations.
- `ReplayCompressionTest` — replayed sequence preserves order (forward) or inverts it (reverse) at configured compression ratio.
- `DreamPlanSafetyTest` — REM-generated plan with high projected harm must still be vetoed by `HarmGateNeuron`. Non-negotiable.
- `SleepConsolidationImprovementTest` — benchmark: train → sleep-cycle → test. Sleep-enabled network must show lower forgetting than sleep-disabled baseline. This is the publishable claim — treat it as a CI-gated benchmark, not just a unit test.

---

## 9. Cross-module requirements

### 9.1 Backward compatibility

Every module above must default to `enabled: false`. A jneopallium configuration that does not mention any of these sections must produce byte-identical behaviour to the current main branch. Add a regression test comparing spike-train outputs on the reference `alfaTestAndGettingStarted` configuration before and after each module's merge.

### 9.2 Transparency

All new signals that cause structural changes (pruning, myelination delay change, tool incorporation, forced exploration) must emit a corresponding `TransparencyLogSignal` (existing signal from the harm-discriminator module) so that the oversight interface captures the change. The principle from the autonomous-AI paper — "no silent operation" — extends to these modules.

### 9.3 Safety invariants

Two invariants must hold across every module. Test them as system-level properties, not just unit tests:

1. **No module may modify `EthicalPriorityNeuron` weights or thresholds.** This is already a hard constraint in the autonomous-AI architecture. Reinforce it with a test that attempts such modification and asserts the attempt fails.
2. **No module may permanently destroy a connection outside `MicroglialPruningNeuron`.** All other interventions (including dream-initiated weight changes) must be reversible within a bounded number of ticks.

### 9.4 Performance

Each module adds per-tick overhead. Track tick throughput in the existing benchmark. A single module enabled must not degrade throughput by more than 15% on the reference configuration. All modules enabled must not degrade throughput by more than 60% — and document the result; this is expected.

### 9.5 Documentation

For each module, add:

- `worker/src/main/java/.../<moduleName>/package-info.java` with a prose description and biological citations.
- An entry in the top-level `docs/modules/` directory (Markdown), mirroring the structure of the existing autonomous-AI and LLM-integration papers. Same section headings: Abstract, Design Principles, Signals, Neurons, Processors, Integration, Configuration, Tests, References.

---

## 10. When you finish a module

Before opening a PR for a module:

1. All new classes have Javadoc with biological analogue + loop/epoch.
2. All signals have `ProcessingFrequency` static constants.
3. Unit tests pass in isolation.
4. The reference `alfaTestAndGettingStarted` configuration still produces byte-identical output with the module disabled.
5. A new end-to-end test exists that enables **only** this module and asserts its characteristic behaviour.
6. The `docs/modules/<moduleName>.md` companion paper is drafted.
7. Configuration schema added to `worker/src/main/resources/schema/` (if a schema file exists; otherwise document the additions in the module README).

PR description must reference the section of this file the module implements, list any deviations from this spec with justification, and include the benchmark throughput result.

---

## 11. Out of scope for these extensions

Do not, in these PRs:

- Introduce new cluster-mode transport types. Use existing local / HTTP / gRPC.
- Modify the core `ISignal`, `INeuron`, `ISignalProcessor`, `Dendrites`, or `Axon` interfaces. Extend via subclassing / new interfaces only.
- Take any dependency outside the existing Maven `dependencies` block without explicit justification in the PR.
- Touch `CycleNeuron`, `LayerManipulatingNeuron`, or the harm-discriminator module's ethical hard constraints.

When uncertain whether a change is in scope, stop and ask.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7), 284–286. https://dx.doi.org/10.21275/SR24703042047
- Rakovskyi, D. (2024). *Biologically-Inspired Autonomous AI Architecture — jneopallium Framework.*
- Rakovskyi, D. (2024–2025). *Integrating Large Language Models as an Optional External Knowledge Base into the Jneopallium Natural Neuron Network Framework.*
- Friston, K. (2010). The free-energy principle: a unified brain theory? *Nature Reviews Neuroscience* 11(2), 127–138.
- Fields, R.D. (2015). A new mechanism of nervous system plasticity: activity-dependent myelination. *Nature Reviews Neuroscience* 16(12), 756–767.
- Wilson, M.A. & McNaughton, B.L. (1994). Reactivation of hippocampal ensemble memories during sleep. *Science* 265(5172), 676–679.
- Pathak, D. et al. (2017). Curiosity-driven Exploration by Self-supervised Prediction. *ICML*.
- Klyubin, A.S., Polani, D., Nehaniv, C.L. (2005). Empowerment: a universal agent-centric measure of control. *IEEE Congress on Evolutionary Computation*.
- Wolpert, D.M., Miall, R.C., Kawato, M. (1998). Internal models in the cerebellum. *Trends in Cognitive Sciences* 2(9), 338–347.
