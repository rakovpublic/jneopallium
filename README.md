# Jneopallium

> A biologically-grounded Java framework for modelling natural neuron networks at any chosen level of detail.

[![License: BSD-3-Clause](https://img.shields.io/badge/License-BSD_3--Clause-blue.svg)](LICENSE.MD)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://openjdk.org/)
[![Build: Maven](https://img.shields.io/badge/build-Maven-C71A36.svg)](pom.xml)
[![DOI](https://img.shields.io/badge/DOI-10.21275%2FSR24703042047-green.svg)](https://dx.doi.org/10.21275/SR24703042047)
[![Topic: neural-networks](https://img.shields.io/badge/topic-neural--networks-blueviolet.svg)](https://github.com/topics/neural-networks)

Jneopallium decouples neuron-network processing logic from the actual neuron and signal types — the same way `java.util.Collections` decouples container algorithms from the elements they hold — via generics and interfaces. The result is a framework where you can model anything from a textbook perceptron to a multi-receptor, multi-timescale, neuromodulator-aware network that mirrors natural cognitive processes.

Published in the *International Journal of Science and Research (IJSR)*, Vol. 13, Issue 7, July 2024.
**Author:** Dmytro Rakovskyi — Kharkiv, Ukraine.

---

## Table of Contents

1. [Why Jneopallium](#why-jneopallium)
2. [Key Features](#key-features)
3. [Project Layout](#project-layout)
4. [Core Abstractions](#core-abstractions)
5. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Build Requirements](#build-requirements)
   - [Clone & Build](#clone--build)
   - [Using as a Dependency](#using-as-a-dependency)
   - [Running](#running)
6. [Modelling Process](#modelling-process)
7. [Signal Processing Frequency](#signal-processing-frequency)
8. [Deployment Modes](#deployment-modes)
9. [Bridge Framework](#bridge-framework)
10. [Autonomous AI Architecture](#autonomous-ai-architecture)
11. [LLM Knowledge-Base Integration](#llm-knowledge-base-integration)
12. [Applications](#applications)
13. [Comparison with Other Frameworks](#comparison-with-other-frameworks)
14. [Roadmap](#roadmap)
15. [Contributing](#contributing)
16. [Citation](#citation)
17. [License](#license)
18. [Contact & Acknowledgements](#contact--acknowledgements)

---

## Why Jneopallium

Most neural-network libraries are built around a fixed mathematical abstraction (matrix multiplications, fixed activation functions, one signal type). Jneopallium is built around a *programmable* abstraction: you define what a signal is, what a neuron is, and how the two interact, then let the framework run the topology and scheduling.

This makes the framework well-suited to:

- modelling neuromodulator behaviour (multiple signal classes with different propagation speeds);
- composing modular neuron networks where one net's output is another net's input;
- experimenting with non-uniform topologies (probabilistic neuron placement, neighbour constraints);
- safety-gated control of real-world systems (see [Bridge Framework](#bridge-framework)).

## Key Features

- **Typed signals** — Define arbitrary signal types (bioelectric, biochemical, custom) with independent propagation characteristics.
- **Receptor heterogeneity** — A neuron may implement multiple interfaces, each handled by a distinct signal processor — multi-receptor behaviour through Java's multiple-interface inheritance.
- **Dual processing loops** — A *fast* loop (every tick) and a *slow* loop (every N ticks) model the speed gap between bioelectric spikes (~100 m/s) and neuromodulatory diffusion (100–1000× slower).
- **Per-signal frequency control** — Each signal type carries a `ProcessingFrequency(loop, epoch)`, producing a hierarchy of natural timescales in a single configuration.
- **Statistical structure generation** — Define per-layer neuron probabilities and neighbouring rules to generate topologies.
- **Dynamic layer sizing** — Create and delete neurons at runtime via `LayerManipulatingNeuron`.
- **Multiple I/O sources** — Attach multiple input sources and output collectors, each with its own processing frequency.
- **Modular composition** — Connect separate neuron networks via `INeuronNetInput`.
- **Discriminator support** — Define any number of discriminators for GAN-style or safety-critical architectures.
- **Deployment modes** — Local, HTTP cluster, or gRPC (FPGA-capable).

## Project Layout

The build is a multi-module Maven project (`com.rakovpublic.jneopallium:wrapper:1.0`).

```
jneopallium/
├── master/         # Cluster master node — coordinates workers, holds shared state
├── worker/         # Worker node — runs neurons, processors, signal chains, bridges
├── doc/            # Design notes, diagrams, architecture documents
├── .github/        # Workflows, issue/PR templates
├── pom.xml         # Aggregator POM (packaging=pom)
├── LICENSE.MD      # BSD 3-Clause
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── SECURITY.md
├── TestPlanPhase1.md / TestPlanPhase2*.md   # Phase test plans
└── WorkDiary.md    # Running design log
```

All bridge adapters live under `worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/`.

## Core Abstractions

| Abstraction               | Role                                                                                       |
| ------------------------- | ------------------------------------------------------------------------------------------ |
| `INeuron`                 | Base interface implemented by every neuron class                                           |
| `ISignalProcessor<S, N>`  | Stateless processor parameterised on signal type `S` and neuron interface `N`              |
| `Neuron`                  | Base implementation providing `Axon` and `Dendrites`                                       |
| `Dendrites`               | Input addresses, accepted signal types, input weights                                      |
| `Axon`                    | Output addresses, signal type, output weight                                               |
| `ISignalChain`            | Ordered processor pipeline invoked during `activate()`                                     |
| `CycleNeuron`             | Controls the fast/slow loop ratio (layer id `Integer.MIN_VALUE`, neuron id `0`)            |
| `LayerManipulatingNeuron` | Creates/deletes neurons dynamically (neuron id `Long.MIN_VALUE` on each layer)             |
| `IInitInput`              | Input source with a default processing frequency                                           |
| `InputInitStrategy`       | Describes how input signals are populated into neurons                                     |
| `INeuronNetInput`         | Wires one neuron network's output as another's input                                       |
| `IOutputAggregator`       | Output destination                                                                         |
| `IConnectionGenerator`    | Strategy for connecting neurons during topology generation                                 |
| `NeighboringRules`        | Constraints on which neuron types may be vertically adjacent                               |

## Getting Started

### Prerequisites

- **Java 17+** (LTS; the framework targets the baseline `java.net.http.HttpClient` API set)
- **Maven 3.9+**
- Optional: Docker (for the planned containerised distributed mode)

### Build Requirements

When extending or forking the project, keep the build-plugin floor consistent with Java 17 support — older versions silently misbehave on JDK 17:

| Plugin                     | Minimum   | Recommended | Notes                                                              |
| -------------------------- | --------- | ----------- | ------------------------------------------------------------------ |
| `maven-compiler-plugin`    | 3.10.1    | **3.13.0**  | Use `<release>17</release>` rather than `<source>/<target>`        |
| `maven-surefire-plugin`    | 3.2.5     | 3.5.x       | Earlier 3.x versions hit module-path / illegal-reflection warnings |
| `maven-jar-plugin`         | 3.3.0     | 3.4.x       | Multi-release JAR handling on 17                                   |
| `maven-javadoc-plugin`     | 3.6.0     | 3.10.x      | Stable Javadoc generation on 17                                    |

Recommended compiler configuration:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>17</release>
    </configuration>
</plugin>
```

`<release>` is preferred over `<source>` + `<target>` because it also constrains the bootclasspath to the target JDK's API — preventing accidental use of newer (e.g. Java 21) methods in Java 17 bytecode.

### Clone & Build

```bash
git clone https://github.com/rakovpublic/jneopallium.git
cd jneopallium
mvn clean install
```

This builds both `master` and `worker` modules and installs the artifacts to your local Maven repository under `com.rakovpublic.jneopallium`.

### Using as a Dependency

Add the worker module to your own project once the artifacts are published (currently install locally, see [Roadmap](#roadmap) for Maven Central plans):

```xml
<dependency>
    <groupId>com.rakovpublic.jneopallium</groupId>
    <artifactId>worker</artifactId>
    <version>1.0</version>
</dependency>
```

### Running

Launch jneopallium pointing it at:

1. a JAR containing your user-defined signals, neurons, and processors;
2. a neuron-network structure file;
3. a configuration file.

A complete walkthrough — 4 signal types, 3 neuron types, full config — lives on the [`test/alfaTestAndGettingStarted`](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted) branch:

- [Functional logic](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions)
- [Structural logic](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions/structurallogic)
- [I/O logic](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/java/com/rakovpublic/jneuropallium/worker/test/definitions/ioutils)
- [Configuration files](https://github.com/rakovpublic/jneopallium/tree/test/alfaTestAndGettingStarted/worker/src/main/resources)

## Modelling Process

Building a model means defining three layers of logic: **functional**, **structural**, and **I/O**.

### 1. Functional Logic — Signals, Neurons, Processors

Define a signal type:

```java
public class IntSignal implements ISignal {
    private int value;
    // getters / setters / weight handling
}
```

Define a neuron interface for one receptor:

```java
public interface NeuronIntField extends INeuron {
    int getInternalField();
    void setInternalField(int value);
}
```

Implement the matching signal processor:

```java
public class IntProcessor implements ISignalProcessor<IntSignal, NeuronIntField> {
    @Override
    public void process(IntSignal signal, NeuronIntField neuron) {
        neuron.setInternalField(neuron.getInternalField() + signal.getValue());
    }
}
```

Implement a neuron — implementing more than one neuron interface gives it more than one receptor:

```java
public class NeuronA extends Neuron implements NeuronIntField, NeuronWithDoubleField {
    // Two receptors: processes both IntSignal and DoubleSignal
}
```

### 2. Structural Logic — Network Topology

Use `NeuronNetStructureGenerator` with:

- a `HashMap<Integer, Long>` of layer sizes;
- a `HashMap` of per-neuron-class appearance probabilities per layer;
- a list of `NeighboringRules` (vertical ordering constraints);
- an `IConnectionGenerator` implementation.

### 3. I/O Logic — Inputs and Outputs

Implement `IInitInput` for each input source and `IOutputAggregator` for each output sink. Each input declares a default processing frequency that can be reconfigured at runtime via `CycleNeuron`.

## Signal Processing Frequency

Two nested loops give fine-grained scheduling control:

- **Fast loop** — every iteration; bioelectric / sensorimotor signals.
- **Slow loop** — every *N* fast iterations; *N* is set via `CycleNeuron`.

Each signal and input source declares a `ProcessingFrequency` with an `int loop` and a `long epoch`. A signal with `loop = 2` is processed every other fast iteration; `epoch` applies the same divisor inside the slow loop.

With a fast/slow ratio of *N* = 10:

| Timescale            | Effective Frequency  |
| -------------------- | -------------------- |
| Fast loop, epoch 1   | every tick           |
| Fast loop, epoch 2   | every 2 ticks        |
| Fast loop, epoch 3   | every 3 ticks        |
| Slow loop, epoch 1   | every 10 ticks       |
| Slow loop, epoch 3   | every 30 ticks       |
| Slow loop, epoch 10  | every 100 ticks      |

## Deployment Modes

| Mode             | Description                                                          |
| ---------------- | -------------------------------------------------------------------- |
| **Local**        | Single-JVM execution                                                 |
| **Cluster HTTP** | Distributed execution over plain HTTP                                |
| **Cluster gRPC** | Distributed execution over gRPC; FPGA targets supported              |

## Bridge Framework

Every adapter between an external real-world system and the Jneopallium signal pipeline is a **bridge**. The shared contract — six ground rules, universal write algorithm, audit schema, acceptance scenarios, phase progression — is specified once in [`00-FRAMEWORK.md`](00-FRAMEWORK.md); per-protocol specs (`01-PLC4X.md` … `14-LTI-XAPI.md`) reference it instead of restating it.

Shared scaffolding under `worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/common/`:

- `AbstractBridgeOutputAggregator` — template-method enforcement of the universal §2.2 write algorithm (interlock → override → clamp → rate-limit → diff-suppress → audit);
- `OverrideRegistry`, `AbstractBridgeAuditOutput`, `BridgeReconnectPolicy`, `BridgeAuditRecord`, `BridgeBindingDirection`.

New bridges go in `worker.bridge.<bridge-id>/` and start every loop in `SHADOW` mode.

### Bridge index

| ID   | Bridge              | Domain            | Status  | Safety ceiling                |
| ---- | ------------------- | ----------------- | ------- | ----------------------------- |
| —    | OPC UA              | industrial        | shipped | AUTONOMOUS (per-loop)         |
| 01   | Apache PLC4X        | legacy PLC        | shipped | AUTONOMOUS (per-loop)         |
| 02   | MQTT + Sparkplug B  | IIoT              | shipped | ADVISORY                      |
| 03   | FMI / FMU           | simulation        | shipped | AUTONOMOUS (sim only)         |
| 04   | ROS 2 / DDS         | robotics          | shipped | ADVISORY initially            |
| 05   | Lab Streaming Layer | BCI / physiology  | spec    | ADVISORY (read-mostly)        |
| 06   | HL7 FHIR            | clinical          | spec    | ADVISORY (regulatory ceiling) |
| 07   | DICOM               | medical imaging   | spec    | READ-ONLY                     |
| 08   | Apache Kafka        | enterprise/cyber  | spec    | ADVISORY                      |
| 09   | OpenTelemetry       | observability     | spec    | EXPORT-ONLY                   |
| 10   | Eclipse Ditto       | digital twins     | shipped | ADVISORY                      |
| 11   | IEC 61850           | power grid        | shipped | READ-ONLY initially           |
| 12   | MAVLink             | drones            | shipped | SIM-ONLY initially            |
| 13   | CANopen             | embedded          | shipped | ADVISORY                      |
| 14   | LTI / xAPI          | adaptive tutoring | spec    | ADVISORY                      |

Common pattern for every bridge: protocol-native reads become typed `MeasurementSignal` / `AlarmSignal` (or domain-specific variants); writes flow back through the universal `AbstractBridgeOutputAggregator` algorithm, are vetoed by the harm gate, and emit an audit record whether applied, clamped, or rejected. Per-protocol nuances (forbidden topics, advisory prefixes, write-index allowlists, structural READ-ONLY ceilings) are enforced both at config load and at runtime. See the per-protocol docs in [`doc/`](doc) for details.

## Autonomous AI Architecture

A companion design derives a full autonomous AI architecture on top of jneopallium:

- **16 signal types** — fast-loop bioelectric signals (`SpikeSignal`, `MotorCommandSignal`, `ErrorSignal`, …) and slow-loop neuromodulators (Dopamine, Serotonin, Norepinephrine, Acetylcholine, GABA).
- **28 neuron classes** across 8 layers: Input (0), Feature Detection (1), Attention & Working Memory (2), Memory & Prediction (3), Planning (4), Action Selection (5), Learning (6), Homeostasis & Regulation (7).
- **11 signal processors** — spike encoding, lateral inhibition, salience computation, predictive coding, STDP, competitive action selection, …
- **Loop-prevention subsystem** — `RegionMonitorNeuron`, `LoopDetectorNeuron`, `LoopCircuitBreakerNeuron`, `OscillationBoundaryNeuron`, `ReentrantGuardNeuron`: four graduated circuit-breaker strategies from weight dampening to connection breaking.
- **Human-harm discriminator** — a consequence model (not an output filter) that simulates projected actions, evaluates them across five welfare dimensions (physical safety, psychological wellbeing, autonomy, social harm, long-term consequences), and vetoes harmful actions before execution. Includes structurally inviolable hard constraints.

## LLM Knowledge-Base Integration

An optional, non-blocking extension lets the network consult a Large Language Model as an external advisory knowledge base:

- **4 signal types:** `LLMQuerySignal`, `LLMResponseSignal`, `LLMConfidenceSignal`, `LLMTimeoutSignal`.
- **3 neuron classes:** `LLMKnowledgeNeuron` (dispatch + caching), `LLMVerificationNeuron` (cross-validation against internal models), `LLMFallbackNeuron` (circuit breaker for graceful degradation).
- **2 signal processors:** `LLMQueryProcessor`, `LLMResponseProcessor`.

Design constraints: LLM processing runs exclusively on the slow loop (never blocks real-time sensorimotor processing), LLM responses are cross-validated against internal models before use, and the system operates at full capability when the LLM is unavailable. Local (Ollama), cloud (OpenAI, Anthropic), and disabled modes are all supported.

## Applications

- **Industrial process control** — drive any OPC UA PLC/SCADA via the Eclipse Milo bridge; neuron-derived setpoints flow through the safety/interlock chain.
- **Robotics** — direct I/O with hardware controllers; sensorimotor loops at biologically-relevant timescales.
- **Autonomous mission control** — decision-making under high latency and mission-flow uncertainty.
- **Neuroscience research** — model neural control structures and structural deviations at a chosen level of detail.
- **Decision modelling** — model organisational decision processes under signals with varying volatility.
- **General AI research** — biologically-inspired architectures aimed at general intelligence.

## Comparison with Other Frameworks

| Framework                                                  | Focus                                  | Key Difference                                                       |
| ---------------------------------------------------------- | -------------------------------------- | -------------------------------------------------------------------- |
| [NEURON Simulator](https://github.com/neuronsimulator/nrn) | High-detail biophysical modelling      | Fixed high detail; jneopallium lets you pick the abstraction level   |
| [CoreNeuron](https://github.com/BlueBrain/CoreNeuron)      | Optimised simulation engine for NEURON | Focused on exact biological replication                              |

Jneopallium's purpose is to bridge neurobiology and computer science with **user-defined abstraction depth**. NEURON and CoreNeuron aim to build exact copies of natural neuron networks; jneopallium aims to let you choose how much biology you want.

## Roadmap

| #  | Phase                                                                                         | Status      |
| -- | --------------------------------------------------------------------------------------------- | ----------- |
| 1  | Core framework (no distribution, no synchronisation)                                          | done        |
| 2  | Java HTTP distributed mode with neuron-network synchronisation                                | done        |
| 3  | gRPC implementation for cluster mode                                                          | in progress |
| 4  | Kafka input-source implementation                                                             | planned     |
| 5  | Maven Central artifacts and hosted Javadoc                                                    | planned     |
| 6  | Docker / Kubernetes containers and Python/shell infra scripts                                 | planned     |
| 7  | Neuron-net graphic designer (Eclipse plugin first; IDEA optional)                             | planned     |
| 8  | Redis as meta storage *(optional)*                                                            | optional    |
| 9  | AWS Lambda distributed mode *(optional)*                                                      | optional    |
| 10 | Amazon-cluster integration *(optional)*                                                       | optional    |

Tool architecture is complete; implementation is ~95% done and has passed pre-alpha. Phase test plans live in [`TestPlanPhase1.md`](TestPlanPhase1.md), [`TestPlanPhase2.md`](TestPlanPhase2.md), and [`TestPlanPhase2Optional.md`](TestPlanPhase2Optional.md). The running design log is [`WorkDiary.md`](WorkDiary.md).

## Contributing

Contributors are welcome — the project is actively looking for them. Start with:

- [`CONTRIBUTING.md`](CONTRIBUTING.md) — how to set up, where to file issues, branch conventions.
- [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) — community standards.
- [`SECURITY.md`](SECURITY.md) — responsible-disclosure policy.

For bridge work in particular, every new bridge must comply with the §2.2 universal write algorithm and ship with audit + override hooks.

## Citation

If you use jneopallium in research, please cite:

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

Further reading:

- [IJSR paper](https://www.ijsr.net/getabstract.php?paperid=SR24703042047)
- [DOU forum thread](https://dou.ua/forums/topic/49673/)

## License

BSD 3-Clause License. See [`LICENSE.MD`](LICENSE.MD) for the full text.

## Contact & Acknowledgements

- **GitHub:** <https://github.com/rakovpublic/jneopallium>
- **GitLab mirror:** <https://gitlab.com/rakovpublic/jneopallium>

Feel free to reach out — the project is actively looking for contributors.

Special thanks to the Department of Informatics at Kharkiv National University of Radio and Electronics, and to Eugen Putiatin, Helen Matat, Tatiana Sinelnikova, and Volodymyr Brytik.
