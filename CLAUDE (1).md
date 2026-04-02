# Jneopallium — Claude Code Guide

## Project Overview

Jneopallium is a Java framework for modeling natural neuron networks with customizable levels of detail. It separates neuron network processing logic from actual neuron and signal types using Java generics and interfaces, similar to how Java Collections separate storage logic from stored object types.

**Repository:** [github.com/rakovpublic/jneopallium](https://github.com/rakovpublic/jneopallium)
**License:** BSD-3-Clause
**Language:** Java 100%
**Author:** Dmytro Rakovskyi (Kharkiv, Ukraine)

---

## Core Abstractions

| Interface / Class | Purpose |
|---|---|
| `INeuron` | Base interface implemented by every neuron class |
| `ISignalProcessor<S, N>` | Stateless processor parameterized on signal type `S` and neuron interface `N` |
| `Dendrites` | Typed, weighted input connectors (input addresses, signal types, weights) |
| `Axon` | Typed, weighted output connectors (output addresses, signal type, weight) |
| `ISignalChain` | Ordered processor pipeline called during `activate()` |
| `Neuron` (abstract class) | Base class to extend; contains Axon and Dendrites |
| `Signal` (interface) | Base for all signal types in the system |

## Processing Architecture

The framework runs **two processing loops**:

- **Fast loop:** Executes every tick. Models bioelectric spike propagation (~100 m/s).
- **Slow loop:** Executes once every N fast ticks (N configured in `CycleNeuron`). Models neuromodulatory/biochemical processes (100–1000× slower).

Each signal type has a `ProcessingFrequency(loop, epoch)`:
- `loop = 1` → fast loop; `loop = 2` → slow loop
- `epoch` determines sub-frequency: epoch 1 = every iteration, epoch 2 = every 2nd, epoch 3 = every 3rd, etc.

### Special Neurons

| Neuron | Layer ID | Purpose |
|---|---|---|
| `CycleNeuron` | Layer `Integer.MIN_VALUE`, Neuron 0 | Controls fast/slow loop ratio. Send signals to layer `-2147483648`, neuron `0` to modify. |
| `LayerManipulatingNeuron` | Each layer, ID `Long.MIN_VALUE` | Creates/deletes neurons via `CreateNeuronSignal` / `DeleteNeuronSignal` |

## Deployment Modes

- **Local:** Single JVM, no distribution
- **Cluster HTTP:** Distributed via HTTP between worker nodes
- **Cluster gRPC:** Distributed via gRPC; supports FPGA deployment

## Project Structure

```
jneopallium/
├── master/          # Orchestration / master node code
├── worker/          # Core framework code
│   └── src/main/java/com/rakovpublic/jneuropallium/worker/
│       ├── net/neuron/          # INeuron, Neuron, Axon, Dendrites, signal processors
│       │   └── impl/
│       │       ├── cycleprocessing/   # CycleNeuron and loop control signals
│       │       └── layersizing/       # LayerManipulatingNeuron
│       └── ...
├── doc/             # Documentation
├── pom.xml          # Maven build (parent POM)
└── README.md
```

## How to Build a Model

### Step 1: Define Signal Types
Create classes implementing the `Signal` interface. Example: `IntSignal`, `DoubleSignal`.

### Step 2: Define Neuron Interfaces
Each processing mechanism gets a separate interface extending `INeuron`. Example: `NeuronIntField`, `NeuronWithDoubleField`.

### Step 3: Implement Signal Processors
Create classes implementing `ISignalProcessor<S, N>` for each signal-neuron pair. Example: `IntProcessor`, `DoubleProcessor`.

### Step 4: Implement Neurons
Extend `Neuron` class and implement one or more neuron interfaces. Multiple interface implementation enables receptor heterogeneity (a neuron processing multiple signal types).

### Step 5: Define Network Structure
Use `NeuronNetStructureGenerator` with:
- HashMap of layer sizes
- HashMap of statistical properties per neuron type (appearance probability)
- List of `NeighboringRules` (vertical structure constraints)
- `IConnectionGenerator` implementation (how neurons connect)

### Step 6: Define I/O
- **Input:** Implement `IInitInput` for each input source. Use `InputInitStrategy` to define signal propagation to neurons. For modular models, implement `INeuronNetInput`.
- **Output:** Implement `IOutputAggregator` for output destinations.

### Step 7: Configure and Launch
Create configuration file, package user code as JAR, and launch jneopallium with paths to: user JAR, network structure file, and configuration file.

## LLM Knowledge Base Integration (Optional Extension)

The framework supports an optional LLM integration as an external advisory knowledge base. This is **non-blocking** and **non-critical** — the system must never rely on LLM as a source of truth.

### New Components

**Signals (all slow loop):**
- `LLMQuerySignal` (epoch 2) — dispatches query with context and priority
- `LLMResponseSignal` (epoch 2) — carries response with confidence
- `LLMConfidenceSignal` (epoch 3) — verified confidence and applicability verdict
- `LLMTimeoutSignal` (epoch 1) — triggers fallback on unavailability

**Neurons (Layer 3):**
- `LLMKnowledgeNeuron` — implements `ILLMCapable extends INeuron`; manages cache, dispatches async queries
- `LLMVerificationNeuron` — trust boundary; cross-validates responses against internal models
- `LLMFallbackNeuron` — circuit breaker (CLOSED → HALF_OPEN → OPEN); routes to internal knowledge when LLM unavailable

**Processors:**
- `LLMQueryProcessor` — `ISignalProcessor<LLMQuerySignal, ILLMCapable>`; async HTTP/gRPC via CompletableFuture
- `LLMResponseProcessor` — `ISignalProcessor<LLMResponseSignal, INeuron>`; writes to WorkingMemory with reduced TTL

### Key Interface

```java
public interface ILLMCapable extends INeuron {
    void submitQuery(LLMQuerySignal query);
    Optional<LLMResponseSignal> getCachedResponse(String queryId);
    boolean isLLMAvailable();
    void setLLMEndpoint(String endpoint);
    void setMaxLatency(long milliseconds);
}
```

### Design Rules
1. LLM queries are **slow loop only** — never block fast loop sensorimotor processing
2. LLM responses are **untrusted** — always cross-validated before use
3. Verified info enters `WorkingMemoryNeuron` with **reduced TTL** (expires faster than internal knowledge)
4. LLM info **never writes directly** to `LongTermMemoryNeuron`
5. All LLM-influenced decisions pass through `HarmGateNeuron`
6. `EthicalPriorityNeuron` hard constraints prevent LLM from modifying harm discriminator weights
7. Default mode is **disabled** — LLM must be explicitly enabled in configuration

### Operational Modes
- **Local:** Connect to Ollama or similar local inference server
- **Cloud:** Connect to OpenAI/Anthropic API (circuit breaker critical)
- **Disabled (default):** No LLM integration; pure internal knowledge

### Configuration (in jneopallium config file)

```yaml
llm:
  enabled: false                    # default: disabled
  endpoint: "http://localhost:11434" # Ollama default, or cloud API URL
  apiKey: ""                        # optional, for cloud providers
  maxLatencyMs: 5000                # timeout threshold
  cacheTtlSeconds: 300              # response cache TTL
  circuitBreaker:
    failureThreshold: 5             # consecutive failures before OPEN
    halfOpenProbeIntervalMs: 30000  # probe interval in HALF_OPEN state
  model: "llama3"                   # model identifier
```

## Autonomous AI Architecture (Reference)

The companion article defines a complete autonomous AI using jneopallium:
- **16 signal types** across fast/slow loops
- **28 neuron classes** across 8 layers (L0–L7)
- **11 signal processors**
- **5 modules:** Perception, Attention+Memory, Planning, Action Selection, Learning
- **Loop prevention:** RegionMonitorNeuron, LoopDetectorNeuron, LoopCircuitBreakerNeuron, OscillationBoundaryNeuron, ReentrantGuardNeuron
- **Harm discriminator:** HarmGateNeuron, ConsequenceModelNeuron, HarmEvaluationNeuron, HarmVetoNeuron, HarmLearningNeuron, HarmContextNeuron, EthicalPriorityNeuron

## Key Articles

1. Rakovskyi D. (2024). "Framework for Natural Neuron Network Modeling: The Jneopallium Approach." IJSR, Vol. 13 Issue 7. DOI: 10.21275/SR24703042047
2. Rakovskyi D. (2024). "Biologically-Inspired Autonomous AI Architecture." Jneopallium Framework Documentation.

## Common Tasks

```bash
# Build the project
mvn clean install

# Run tests (test branch)
git checkout test/alfaTestAndGettingStarted
mvn test

# Package user model as JAR
mvn package -pl worker
```

## Tips for Contributing

- The project uses Maven for builds
- Core interfaces are in the `worker` module
- Follow the INeuron / ISignalProcessor pattern for new neuron and signal types
- Use Java generics to maintain type safety between signals and processors
- Test branch `test/alfaTestAndGettingStarted` contains example implementations
- The project is looking for contributors — see CONTRIBUTING.md
