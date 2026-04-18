# Module B — Embodiment / proprioception

## Abstract

This module adds a body-schema representation and a cerebellar-style efference-copy circuit: every motor command spawns a parallel predicted-outcome signal, which a reafference comparator later matches against the actual proprioceptive feedback. Mismatches surface to the harm discriminator as a free mechanical-failure channel. The module also supports runtime tool incorporation (tools extend the body schema) and damage tracking per effector.

## Design principles

- **Branch-point parity.** `EfferenceCopyNeuron` sits between `PlanningNeuron` and `HarmGateNeuron` so every motor intention spawns a copy before execution.
- **Reversible tool incorporation.** `ToolIncorporationNeuron` stores the prior effector state and restores it on release; if the slot had no prior state, it is removed.
- **Safety reuse.** Proprioceptive mismatch is emitted as `HarmFeedbackSignal(source="mechanical")` — no new logic on the harm side.

## Signals

| Signal | Loop / Epoch | Purpose |
|---|---|---|
| `ProprioceptiveSignal` | 1 / 1 | Effector joint states + timestamp. |
| `EfferenceCopySignal` | 1 / 1 | Predicted outcome of a motor command. |
| `BodySchemaUpdateSignal` | 2 / 3 | Per-effector capability update. |
| `SensorimotorContingencySignal` | 1 / 2 | Action ↔ sensory-delta binding. |

## Neurons

| Neuron | Layer | Role |
|---|---|---|
| `EfferenceCopyNeuron` | 4 | Produces `EfferenceCopySignal` from `MotorCommandSignal`. |
| `BodySchemaNeuron` | 2 | Maintains `BodySchema` map. |
| `ToolIncorporationNeuron` | 2 | Extends body schema with tools. |
| `ReafferenceComparatorNeuron` | 1 | Proprioceptive-vs-predicted mismatch → harm feedback. |

## Integration

- `ReafferenceComparatorNeuron` emits `HarmFeedbackSignal` with `source="mechanical"` when RMS mismatch ≥ `failureEmitThreshold`.
- `BodySchemaNeuron.onProprioceptive` auto-registers previously-unseen effectors.
- `ToolIncorporationNeuron` records the prior schema slot; `release()` restores it or removes the slot entirely if the tool occupied a fresh slot.

## Configuration

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

## Tests

`EmbodimentModuleTest` covers efference-copy generation, reafference mismatch, tool incorporation/release, proprioceptive auto-registration, signal copy semantics, and config defaults.

## References

- Wolpert, D.M., Miall, R.C., Kawato, M. (1998). Internal models in the cerebellum. *TICS* 2(9), 338–347.
- Maravita, A. & Iriki, A. (2004). Tools for the body (schema). *TICS* 8(2), 79–86.
