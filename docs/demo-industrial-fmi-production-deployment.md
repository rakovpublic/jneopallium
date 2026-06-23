# Industrial Loop Guardian Production Deployment

This deployment mirrors the cybersecurity production launch shape, but the
industrial model remains advisory-first for maintenance, diagnosis, and energy
saving. PLC/PID/SIS controls remain deterministic and authoritative; the
Jneopallium model is the supervisory Loop Guardian above them.

## 1. Production Launch Architecture

Launch the worker with the same four `Entry` arguments:

```text
mode model-jar-url context-class context-json
```

For local advisory replay:

```bash
java -cp worker/target/worker-jar-with-dependencies.jar \
  com.rakovpublic.jneuropallium.worker.Entry \
  local \
  file:///opt/jneopallium/industrial-loop-guardian.jar \
  com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext \
  /opt/jneopallium/model/industrial-loop-guardian/production-context.json
```

`Entry` deserializes `context-json` with `context-class`, loads the model JAR,
then maps `local`, `http`, or `grpc` to `LocalApplication`,
`HttpClusterApplication`, or the GRPC server path.

## 2. Custom Model JAR Contents

Package these runtime classes and resources:

| Component | Contents |
| --- | --- |
| Typed signals | `MeasurementSignal`, `AlarmSignal`, `DegradationSignal`, `EfficiencySignal`, `MaintenanceWindowSignal`, `SetpointSignal`, `InterlockSignal` |
| Neurons | `SensorNeuron`, `MeasurementValidatorNeuron`, `OscillationMonitorNeuron`, `DegradationModelNeuron`, `MaintenanceSchedulingNeuron`, `EnergyAccountingNeuron`, `SetpointOptimiserNeuron`, `SafetyGateNeuron` with logical roles such as `PumpHealthAndEfficiencyNeuron`, `OscillationDiagnosisNeuron`, `SensorFaultDiscriminationNeuron`, `EconomicBasisNeuron`, and `SafetyEnvelopeNeuron` |
| Processors | validation, oscillation/stiction diagnosis, degradation scheduling, maintenance-window scheduling, efficiency/economic optimisation, safety gate, dispatch |
| Sources | OPC UA measurements/alarms, MQTT health telemetry, Kafka shadow stream, CMMS maintenance context, energy-meter context |
| Sinks | JSONL audit sink, Kafka/SIEM/webhook adapter, OPC UA command sink only when a separate safety case allows it |

The production context sets `configuration.neuronnet.classes`; `LocalApplication`
checks every class at startup and fails fast if the JAR is incomplete.

## 3. Runtime Context Keys

The generated context is
`worker/src/main/resources/model/industrial-loop-guardian/production-context.json`
and uses the standard shape:

```json
{"properties": {"configuration.isteacherstudying": "false"}}
```

Critical advisory-loop settings:

| Key | Production value |
| --- | --- |
| `configuration.isteacherstudying` | `false` |
| `configuration.discriminatorsAmount` | `0` |
| `configuration.infiniteRun` | `true` |
| `configuration.runoncein` | `1000` milliseconds |
| `configuration.processing.frequency.map` | fast measurements every loop, maintenance/energy slower |
| `industrial.autonomousAction` | `false` |
| `industrial.neuronOwnedLogic` | diagnosis, economic basis, safety envelope, bounded recommendation |

Together, `isteacherstudying=false`, `discriminatorsAmount=0`, and
`infiniteRun=true` put the worker into the continuous advisory loop.
`runoncein=1000` paces that loop to one advisory tick per second before the
per-signal frequency map fans fast and slow signals out.

## 4. Neuron-Net Structure

The trained package is under
`worker/src/main/resources/model/industrial-loop-guardian/`:

| File | Layer | Size | Purpose |
| --- | ---: | ---: | --- |
| `layer-0.json` | 0 | 0 | OPC UA, MQTT, FMI replay, Kafka shadow input boundary |
| `layer-1-fast-telemetry.json` | 1 | 7 | validation, interlocks, override, fast loop state |
| `layer-2-maintenance-energy.json` | 2 | 4 | trained diagnostic heads for pump wear, oscillation/stiction, sensor drift, energy |
| `layer-3-advisory-planning.json` | 3 | 4 | maintenance scheduling, bounded pump trim, economic basis, fixed safety gate |
| `result-layer.json` | 4 | 2 | JSONL maintenance and energy advisories |

Every neuron entry includes `processorMap`, `axon.connectionMap`,
`signalChain`, and trained parameters where applicable. Diagnostic heads also
include `logicalNeuronRole`, `featureGate`, and `ownedReasoning` so production
reviewers can see that the logic is encapsulated in the Jneopallium model.

## 5. Event Sources And Streaming Input

Batch replay uses the deterministic FMI traces. Production should plug in a
site-specific `IInitInput` that converts Kafka, OPC UA, or MQTT records into
typed industrial signals while preserving event time.

Recommended Kafka topic groups:

| Topic group | Signal |
| --- | --- |
| `plant.telemetry.measurements` | `MeasurementSignal` |
| `plant.telemetry.alarms` | `AlarmSignal` |
| `plant.maintenance.events` | `DegradationSignal` / maintenance context |
| `plant.energy.meters` | `EfficiencySignal` |
| `plant.cmms.workorders` | `MaintenanceWindowSignal` / maintenance history |

Keep streaming cadence aligned with `configuration.processing.frequency.map`:
fast loop tags every loop, maintenance and energy findings every 10-60 loops.

## 6. Output Aggregator And Advisory Contract

The advisory sink emits JSONL records:

```json
{
  "asset": "P-101",
  "finding": "pump wear and cavitation risk",
  "confidence": 0.73,
  "evidence": {"neuron": "PumpHealthAndEfficiencyNeuron", "maxVibrationRms": 4.3},
  "recommendation": "SCHEDULE_PUMP_INSPECTION",
  "recommendedAction": "Inspect P-101 impeller and suction strainer.",
  "urgencyHours": 48,
  "economicBasis": {
    "neuron": "EconomicBasisNeuron",
    "estimatedAvoidedShutdownValueUsd": 20000,
    "safetyEnvelopeSatisfied": true
  },
  "safetyEnvelopeSatisfied": true,
  "controlBoundary": {
    "plcPidSis": "deterministic millisecond control and hard safety remain authoritative",
    "jneopallium": "supervisory diagnosis, optimisation, and bounded recommendations"
  },
  "autonomousAction": false
}
```

Forward JSONL to SIEM, Kafka, CMMS, or a webhook. OPC UA actuator writes remain
separate and must stay blocked until an offline replay, shadow pilot, advisory
subscription, and site safety case have all passed.
