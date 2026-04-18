# Module C — Intrinsic motivation / curiosity

## Abstract

Adds intrinsic-reward machinery so an agent explores even without extrinsic reward. Four complementary drives are implemented: novelty (hippocampal CA1-like), learning progress (Oudeyer & Kaplan), empowerment (Klyubin), and boredom-driven inhibition of return. Depends on the embodiment module for empowerment's forward-model inputs.

## Design principles

- **Composable drives.** Each drive is one neuron + one signal; higher-level policy is free to combine them.
- **Pure state on the neuron.** Bloom filters, EMA windows, and visit counts live on the neurons; processors remain stateless.
- **Safety-preserving.** Curiosity only biases action-selection; it cannot bypass the harm gate.

## Signals

| Signal | Loop / Epoch | Purpose |
|---|---|---|
| `NoveltySignal` | 1 / 2 | Novelty score + context hash. |
| `LearningProgressSignal` | 2 / 2 | Domain + error derivative. |
| `EmpowermentSignal` | 2 / 3 | State id + mutual-information estimate. |
| `BoredomSignal` | 2 / 2 | Familiarity for a context. |

## Neurons

| Neuron | Layer | Role |
|---|---|---|
| `NoveltyDetectorNeuron` | 1 | Decaying Bloom filter over context hashes. |
| `LearningProgressNeuron` | 6 | Per-domain dError/dt → intrinsic reward when decreasing. |
| `EmpowermentNeuron` | 4 | Empowerment from sampled action rollouts. |
| `BoredomNeuron` | 2 | Visit counts → attention suppression on over-familiar contexts. |

## Integration

`ActionSelectionNeuron` adds a dendrite for `NoveltySignal` and `EmpowermentSignal` and modifies its softmax:

```
score = extrinsic + β_nov · novelty + β_emp · empowerment
```

`BoredomNeuron` emits `AttentionGateSignal(suppress=true)` for contexts whose familiarity has crossed the configured threshold, forcing the planner to explore.

## Configuration

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

## Tests

`CuriosityModuleTest` covers novelty decay, learning-progress reward sign, empowerment ordering over rollout diversity, boredom saturation and suppression gate, signal copy/clamp semantics, and config defaults.

## References

- Lisman, J.E. & Grace, A.A. (2005). The hippocampal-VTA loop. *Neuron* 46(5), 703–713.
- Oudeyer, P-Y. & Kaplan, F. (2007). What is intrinsic motivation? *Frontiers in Neurorobotics* 1:6.
- Klyubin, A.S., Polani, D., Nehaniv, C.L. (2005). Empowerment. *IEEE CEC*.
- Pathak, D. et al. (2017). Curiosity-driven Exploration by Self-supervised Prediction. *ICML*.
