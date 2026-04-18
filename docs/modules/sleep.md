# Module E — Sleep / replay / offline consolidation

## Abstract

Implements a circadian sleep controller, hippocampal replay, sharp-wave ripples targeting long-term memory, and REM-sleep dreaming. Depends on the glia module (sleep-gated myelination boost) and on the existing `CircadianNeuron`. The headline publishable claim is reduced forgetting when the sleep cycle is enabled on the reference benchmark.

## Design principles

- **Safety-preserving.** REM-generated plans are marked `priority=low, source=DREAM` and must still pass through `HarmGateNeuron` before any motor execution. The test `DreamPlanSafetyTest`-equivalent asserts unsafe dreams are rejected at the planner.
- **Gate, don't modify.** During NREM the fast-loop signal magnitudes are attenuated by `1 - depth`. The gate is configurable, not hard-coded.
- **Single ownership.** Only `SleepControllerNeuron` owns the phase state; all other sleep neurons query it.

## Signals

| Signal | Loop / Epoch | Purpose |
|---|---|---|
| `SleepStateSignal` | 2 / 10 | Current phase + depth. |
| `ReplaySignal` | 2 / 3 | Compressed episode replay. |
| `SharpWaveRippleSignal` | 2 / 1 | Compressed burst replay to LTM. |
| `DreamSignal` | 2 / 5 | REM-generated recombined episode. |

## Neurons

| Neuron | Layer | Role |
|---|---|---|
| `SleepControllerNeuron` | 7 | Advances the WAKE → NREM2 → NREM3 → REM cycle. |
| `HippocampalReplayNeuron` | 3 | Top-K salience-ordered episode replay. |
| `SharpWaveRippleNeuron` | 3 | NREM3-only burst replay. |
| `REMDreamingNeuron` | 3 | REM recombination with planner-safety gate. |

## Integration

- During NREM3, `MyelinationNeuron.setConsolidationBoost` is scaled by `sleep.consolidation-boost` (default 3.0), accelerating offline myelination.
- `MetaplasticityNeuron` runs at 3x normal rate under NREM3.
- REM dreams whose novelty exceeds `dreaming.max-novelty-for-planning` are filtered out at `REMDreamingNeuron.isPlanningCandidate(...)`, ensuring wild recombinations never reach the planner as candidate plans.
- Consolidation fast-path: `SharpWaveRippleSignal` is recognized by `LongTermMemoryNeuron`'s consolidation processor and treated with elevated importance.

## Configuration

```yaml
sleep:
  enabled: false
  circadian:
    cycle-ticks: 10000
    nrem-fraction: 0.6
    rem-fraction: 0.15
  replay:
    direction: REVERSE
    compression-ratio: 10.0
    top-k-episodes: 20
  dreaming:
    recombination-count: 5
    max-novelty-for-planning: 0.7
  consolidation-boost: 3.0
```

## Tests

`SleepModuleTest` covers: full-cycle phase visit (WAKE → NREM2 → NREM3 → REM ordering), NREM3 deeper than NREM2, replay reverse/forward correctness, top-K salience ordering, NREM3 gating of sharp-wave ripples, REM-only dreaming, dream-safety gate at the planner, signal ProcessingFrequency correctness, and config defaults.

## References

- Wilson, M.A. & McNaughton, B.L. (1994). Reactivation of hippocampal ensemble memories during sleep. *Science* 265(5172), 676–679.
- Foster, D.J. & Wilson, M.A. (2006). Reverse replay. *Nature* 440, 680–683.
- Buzsáki, G. (2015). Hippocampal sharp wave-ripple. *Hippocampus* 25(10), 1073–1188.
- Wamsley, E.J. & Stickgold, R. (2011). Memory, sleep, and dreaming. *Sleep Medicine Clinics* 6(1), 97–108.
- Diekelmann, S. & Born, J. (2010). The memory function of sleep. *Nature Rev. Neurosci.* 11, 114–126.
- Saper, C.B. et al. (2005). Hypothalamic regulation of sleep and circadian rhythms. *Nature* 437, 1257–1263.
