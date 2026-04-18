# Module D — Glial support layer

## Abstract

Adds astrocytic, microglial, and oligodendrocyte-analogue neurons plus the headline capability: **activity-dependent per-connection propagation delay** via `DelayedAxon`. This is genuinely new infrastructure — the existing `Axon` class is unchanged; a subclass carries a per-target delay map, and a new `DelayQueue` holds signals until their release tick.

## Design principles

- **No core modification.** `Axon` is untouched; `DelayedAxon extends Axon` adds the delay map. A dispatcher wrapper uses `DelayQueue` to release delayed signals.
- **Myelination never hurts.** The accelerate operation reduces delay monotonically and cannot push below `min-delay-ticks`. Demyelination restores baseline but never exceeds it.
- **Pruning is cautious.** `MicroglialPruningNeuron` honors `min-inactivity-ticks` and a per-epoch cap on prunings.
- **Single source of destruction.** Only `MicroglialPruningNeuron` emits `PruningSignal`; no other module is authorized to sever a connection.

## Signals

| Signal | Loop / Epoch | Purpose |
|---|---|---|
| `CalciumWaveSignal` | 2 / 1 | Astrocytic regional activity. |
| `GliotransmitterSignal` | 2 / 2 | Glutamate / ATP / D-serine release. |
| `PruningSignal` | 2 / 5 | Prune request from microglia. |
| `MyelinationSignal` | 2 / 10 | Update delay on a connection. |

## Neurons

| Neuron | Role |
|---|---|
| `AstrocyteNeuron` | Integrates local activity; emits calcium waves and gliotransmitters. |
| `MicroglialPruningNeuron` | Inactivity-tracking pruning with a per-epoch cap. |
| `MyelinationNeuron` | Counts usage per window and issues delay-reduction signals. |

## Integration

- `DelayedAxon.getDelay(target)` is inspected by the dispatcher wrapper; if non-zero, the signal is enqueued in `DelayQueue` with `releaseAtTick = currentTick + delay`.
- `MetaplasticityNeuron` continues to adjust weights; `MicroglialPruningNeuron` owns actual connection removal.
- `MyelinationNeuron.applyTo(DelayedAxon, MyelinationSignal)` respects the clamp `[min, baseline]`.

## Configuration

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

## Tests

`GliaModuleTest` covers delay clamping, monotonic acceleration, demyelination baseline, `DelayQueue` release semantics at exact tick, calcium-wave emission, microglial safety window and epoch cap, myelination floor behavior, signal copy/frequency assertions, and config defaults.

## References

- Fields, R.D. (2015). Activity-dependent myelination. *Nature Rev. Neurosci.* 16(12), 756–767.
- Gibson, E.M. et al. (2014). Neuronal activity promotes oligodendrogenesis and adaptive myelination. *Science* 344(6183), 1252304.
- Schafer, D.P. et al. (2012). Microglia sculpt postnatal neural circuits. *Neuron* 74(4), 691–705.
- Araque, A. et al. (2014). Gliotransmitters travel in time and space. *Neuron* 81(4), 728–739.
- Volterra, A. & Meldolesi, J. (2005). Astrocytes: From brain glue to communication elements. *Nature Rev. Neurosci.* 6, 626–640.
