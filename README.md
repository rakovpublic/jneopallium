# Jneopallium

A biologically-grounded Java framework for modeling natural neuron networks with customizable levels of detail.

[![License](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11%2B-orange.svg)]()
[![DOI](https://img.shields.io/badge/DOI-10.21275%2FSR24703042047-green.svg)](https://dx.doi.org/10.21275/SR24703042047)

## Overview

Jneopallium is a modular framework that separates neuron network processing logic from actual neuron and signal types — much like Java Collections separate storage logic from the objects they store — using generics and interfaces. It enables researchers and engineers to build neural models at any level of biological detail, from simple perceptron-like abstractions to complex multi-signal, multi-receptor architectures that mirror natural cognitive processes.

The framework was published in the *International Journal of Science and Research (IJSR)*, Volume 13, Issue 7, July 2024.

**Author:** Dmytro Rakovskyi — Jneopallium, Kharkiv, Ukraine

## Key Features

- **Typed signals** — Define arbitrary signal types (bioelectrical, biochemical, or custom) with independent propagation characteristics.
- **Receptor heterogeneity** — Neurons can implement multiple interfaces, each handled by a distinct signal processor, enabling multi-receptor behavior via Java's multiple interface inheritance.
- **Dual processing loops** — A fast loop (every tick) and a slow loop (every N fast ticks) model the speed difference between bioelectric spikes (~100 m/s) and neuromodulatory diffusion (100–1000× slower).
- **Per-signal frequency control** — Each signal type specifies a `ProcessingFrequency(loop, epoch)`, creating a hierarchy of natural timescales within a single configuration.
- **Statistical structure generation** — Define neuron distribution probabilities per layer and neighboring rules to generate network topologies.
- **Dynamic layer sizing** — Create and delete neurons at runtime via `LayerManipulatingNeuron`.
- **Multiple I/O sources** — Attach multiple input sources and output collectors, each with independent processing frequencies.
- **Modular architecture** — Connect separate neuron networks via `INeuronNetInput` to build modular, composable models.
- **Discriminator support** — Define any number of discriminators for GAN-style or safety-critical architectures.
- **Deployment modes** — Run locally, in an HTTP cluster, or via gRPC (including FPGA targets).

## Core Abstractions

| Abstraction | Role |
|---|---|
| `INeuron` | Base interface implemented by every neuron class |
| `ISignalProcessor<S, N>` | Stateless processor parameterized on signal type `S` and neuron interface `N` |
| `Neuron` | Base class providing `Axon` and `Dendrites` |
| `Dendrites` | Encapsulates input addresses, signal types, and input weights |
| `Axon` | Encapsulates output addresses, signal type, and output weight |
| `ISignalChain` | Ordered processor pipeline invoked during `activate()` |
| `CycleNeuron` | Controls the fast/slow loop ratio (layer id `−2147483648`, neuron id `0`) |
| `LayerManipulatingNeuron` | Creates/deletes neurons dynamically (neuron id `−9223372036854775808` at each layer) |
| `IInitInput` | Defines an input source with a default processing frequency |
| `InputInitStrategy` | Describes how input signals propagate to neurons |
| `INeuronNetInput` | Input interface for connecting one neuron network's output to another |
| `IOutputAggregator` | Defines an output destination |
| `IConnectionGenerator` | Describes how to connect neurons during structure generation |
| `NeighboringRules` | Constrains which neuron types may be adjacent (vertical structure) |

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven or Gradle

### Building

```bash
git clone https://github.com/rakovpublic/jneopallium.git
cd jneopallium
# Build the framework
mvn clean install
```

### Modeling Process

Building a model involves three steps: defining functional logic, structural logic, and I/O logic.

#### 1. Functional Logic — Signals, Neurons, and Processors

**Define signal types** — Create classes representing signals in your system and the weight object used for learning.

```java
// Example: a signal carrying an integer value
public class IntSignal implements ISignal {
    private int value;
    // ...
}
```

**Define neuron interfaces** — Each processing mechanism gets a separate interface extending `INeuron`.

```java
public interface NeuronIntField extends INeuron {
    int getInternalField();
    void setInternalField(int value);
}
```

**Implement signal processors** — Each processor implements `ISignalProcessor<S, N>` for a specific signal–neuron pair.

```java
public class IntProcessor implements ISignalProcessor<IntSignal, NeuronIntField> {
    @Override
    public void process(IntSignal signal, NeuronIntField neuron) {
        // Processing logic
    }
}
```

**Implement neurons** — Extend `Neuron` and implement one or more neuron interfaces. A neuron implementing multiple interfaces has multiple receptors and can process different signal types.

```java
// Neuron processing both IntSignal and DoubleSignal
public class NeuronA extends Neuron implements NeuronIntField, NeuronWithDoubleField {
    // Two receptors — processes both signal types
}
```

#### 2. Structural Logic — Network Topology

Use `NeuronNetStructureGenerator` with:
- A `HashMap` of layer sizes.
- A `HashMap` of statistical properties per neuron type (probability of appearance on each layer).
- A list of `NeighboringRules` (vertical neuron ordering constraints).
- An `IConnectionGenerator` implementation (connection strategy).

#### 3. I/O Logic — Inputs and Outputs

Implement `IInitInput` for each input source and `IOutputAggregator` for output destinations. Each input has a default processing frequency modifiable at runtime via `CycleNeuron`.

### Running

Launch jneopallium with paths to your user-defined code JAR, neuron network structure file, and configuration file. See the [configuration examples](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/resources) for reference.

## Signal Processing Frequency

The framework provides fine-grained frequency control through two nested loops:

- **Fast loop** — Processes every iteration. Suitable for bioelectric/sensorimotor signals.
- **Slow loop** — Processes once every N fast-loop iterations (N is configurable via `CycleNeuron`).

Each signal type and input source defines a `ProcessingFrequency` with an integer `loop` field and a long `epoch` field. A signal with `loop = 2` processes once every 2 fast-loop iterations; `epoch` applies the same logic to the slow loop.

With a fast/slow ratio of N = 10, this creates timescales like:

| Timescale | Frequency |
|---|---|
| Fast loop, epoch 1 | Every tick |
| Fast loop, epoch 2 | Every 2 ticks |
| Fast loop, epoch 3 | Every 3 ticks |
| Slow loop, epoch 1 | Every 10 ticks |
| Slow loop, epoch 3 | Every 30 ticks |
| Slow loop, epoch 10 | Every 100 ticks |

## Deployment Modes

| Mode | Description |
|---|---|
| **Local** | Single-machine execution |
| **Cluster HTTP** | Distributed execution over HTTP |
| **Cluster gRPC** | Distributed execution over gRPC; supports FPGA deployment |

## Example Code

A complete working example with 4 signal types and 3 neuron types is available on the [test branch](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted):

- [Functional logic definitions](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions)
- [Structural logic definitions](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions/structurallogic)
- [I/O logic definitions](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions/ioutils)
- [Configuration files](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/resources)

## Autonomous AI Architecture

A companion article derives a full autonomous AI architecture on top of jneopallium, featuring:

- **16 signal types** spanning fast-loop bioelectric signals (SpikeSignal, MotorCommandSignal, ErrorSignal, etc.) and slow-loop neuromodulatory signals (Dopamine, Serotonin, Norepinephrine, Acetylcholine, GABA).
- **28 neuron classes** organized across 8 layers: Input (Layer 0), Feature Detection (Layer 1), Attention & Working Memory (Layer 2), Memory & Prediction (Layer 3), Planning (Layer 4), Action Selection (Layer 5), Learning (Layer 6), and Homeostasis & Regulation (Layer 7).
- **11 signal processors** covering spike encoding, lateral inhibition, salience computation, predictive coding, STDP, competitive action selection, and more.
- **Loop prevention subsystem** with RegionMonitorNeuron, LoopDetectorNeuron, LoopCircuitBreakerNeuron, OscillationBoundaryNeuron, and ReentrantGuardNeuron — four graduated circuit-breaker strategies from weight dampening to connection breaking.
- **Human-harm discriminator module** — A consequence model (not an output filter) that simulates projected actions, evaluates human welfare across five dimensions (physical safety, psychological wellbeing, autonomy, social harm, long-term consequences), and vetoes harmful actions before execution. Includes hard constraints that are structurally inviolable.

## LLM Knowledge Base Integration

An optional, non-blocking extension integrates Large Language Models as an external advisory knowledge base:

- **4 new signal types:** LLMQuerySignal, LLMResponseSignal, LLMConfidenceSignal, LLMTimeoutSignal.
- **3 new neuron classes:** LLMKnowledgeNeuron (query dispatch + caching), LLMVerificationNeuron (cross-validation against internal models), LLMFallbackNeuron (circuit breaker for graceful degradation).
- **2 new signal processors:** LLMQueryProcessor, LLMResponseProcessor.

Design principles: all LLM processing runs on the slow loop (never blocking real-time sensorimotor processing), LLM responses are always cross-validated against internal models before use, and the system operates at full capability when the LLM is unavailable. Supports local (Ollama), cloud (OpenAI, Anthropic), and disabled modes.

## Applications

- **Robotics** — Direct I/O with hardware controllers; model sensorimotor loops at biologically-relevant timescales.
- **Autonomous mission control** — Decision-making under high latency and mission-flow uncertainty.
- **Neuroscience research** — Model neural control structures and structural deviations at a chosen level of detail.
- **Company management** — Model decision processes under signals and metrics with varying volatility.
- **General AI research** — Build toward general intelligence using biologically-inspired architectures.

## Competitors

| Framework | Focus | Key Difference |
|---|---|---|
| [NEURON Simulator](https://github.com/neuronsimulator/nrn) | High-detail biophysical modeling | Fixed high detail; jneopallium lets you choose the detail level |
| [CoreNeuron](https://github.com/BlueBrain/CoreNeuron) | Optimized simulation engine for NEURON | Same as above; focused on exact biological replication |

Jneopallium's main purpose is to bridge neurobiology and computer science with user-defined abstraction depth. NEURON Simulator and CoreNeuron aim to build exact copies of natural neuron networks.

## Repository

- **GitHub:** [https://github.com/rakovpublic/jneopallium](https://github.com/rakovpublic/jneopallium)
- **GitLab:** [https://gitlab.com/rakovpublic/jneopallium](https://gitlab.com/rakovpublic/jneopallium)

## Citation

```bibtex
@article{rakovskyi2024jneopallium,
  title   = {Framework for Natural Neuron Network Modeling: The Jneopallium Approach},
  author  = {Rakovskyi, Dmytro},
  journal = {International Journal of Science and Research (IJSR)},
  volume  = {13},
  number  = {7},
  year    = {2024},
  doi     = {10.21275/SR24703042047},
  issn    = {2319-7064}
}
```

## License

BSD 3-Clause License. See [LICENSE](LICENSE) for details.
