# Module A — Affect subsystem

## Abstract

The affect module adds a lightweight limbic analogue to the jneopallium framework: fast valence/arousal tagging (amygdala-like), slow interoceptive integration (insula-like), cross-system broadcast of affective state, and diffuse modulation of learning-rate and harm thresholds. Default is `enabled: false`; when disabled the module is byte-identical to baseline.

## Design principles

- **No silent operation.** All state changes emit the standard transparency/log signal wherever the signal would cause a structural or threshold change.
- **State lives on the neuron, not the processor.** Processors are stateless; `AmygdalaValenceNeuron`, `AnteriorInsulaNeuron`, `AffectIntegrationNeuron`, and `AffectModulationNeuron` hold all state.
- **Hard safety constraint preserved.** The module never modifies the `EthicalPriorityNeuron` weights or the hard harm thresholds. It multiplies a dynamic "recency bias" factor only.

## Signals

| Signal | Loop / Epoch | Purpose |
|---|---|---|
| `AffectStateSignal` | 2 / 1 | Broadcast valence/arousal summary. |
| `InteroceptiveSignal` | 1 / 2 | Energy, homeostatic error, pain. |
| `AppraisalSignal` | 1 / 2 | Goal delta, novelty, controllability. |

## Neurons

| Neuron | Layer | Role |
|---|---|---|
| `AmygdalaValenceNeuron` | 2 | Fast threat/reward tagging of `SpikeSignal`. |
| `AnteriorInsulaNeuron` | 2 | Integrates interoceptive streams (EMA). |
| `AffectIntegrationNeuron` | 2 | Appraisal × interoception → `AffectStateSignal`. |
| `AffectModulationNeuron` | 7 | Broadcast modulator: learning rate, harm thresholds. |

## Processors

- `InteroceptionProcessor` — updates the insula EMA.
- `ValenceTaggingProcessor` — tags `SpikeSignal` with valence.
- `AffectIntegrationProcessor` — feeds appraisal into amygdala valence.

## Integration

`AffectModulationNeuron` produces modulation factors:

- `shortTermLearningScale = 1 + arousal`
- `longTermConsolidationScale = 1 − 0.5·arousal`
- `harmThresholdMultiplier = clamp((1 − valence)/2 + arousal, 1.0, 5.0)`

## Configuration

```yaml
affect:
  enabled: false
  valence-decay-ticks: 300
  arousal-decay-ticks: 150
  harm-threshold-clamp: [1.0, 5.0]
```

## Tests

`AffectModuleTest` covers signals, neurons, processors, and config defaults.

## References

- LeDoux, J.E. (1998). *The Emotional Brain*. Simon & Schuster.
- Craig, A.D. (2002). How do you feel? Interoception and the anterior insula. *Nature Reviews Neuroscience* 3, 655–666.
- Russell, J.A. (1980). A circumplex model of affect. *J. Personality and Social Psychology* 39, 1161–1178.
- Damasio, A. (1994). *Descartes' Error*. Putnam.
