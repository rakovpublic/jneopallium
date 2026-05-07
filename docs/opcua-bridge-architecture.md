# Jneopallium OPC UA bridge — architecture

The bridge connects Jneopallium to any OPC UA server (PLC, SCADA,
simulator, digital twin) via Eclipse Milo `1.1.1`. It reads field values
into typed industrial signals, runs them through the existing
safety-gated cognitive-control pipeline, and writes neuron-derived
decisions back to the plant — only ever via the safety chain.

## §0 Ground rules (verbatim from the spec)

1. **No raw write to a field actuator from neuron output.** Every write
   goes through the chain:
   `PlanningNeuron → SafetyGateNeuron → SafetyMode check → OperatorOverrideSignal check → OpcUaCommandOutputAggregator`.
   The aggregator itself rejects any `ActuatorCommandSignal` whose
   `execute=false` (shadow) or whose loop is in shadow mode by config.
2. **Interlocks have direct authority.** If
   `InterlockSignal.tripped == true`, the aggregator MUST write the loop
   to its fail-safe value regardless of anything else in the same tick.
   This is the only permitted bypass.
3. **Operator override wins.** When `OperatorOverrideSignal` is active
   for a tag, the aggregator does not write that tag from any
   neuron-derived signal for the duration of the override. Override
   applies to *regulatory* control, not to interlocks.
4. **Every write produces an audit record.** Each accepted, suppressed,
   or rejected write emits a transparency-log line that is persisted via
   `OpcUaTransparencyLogOutput` to a configurable OPC UA audit node *and*
   a local file. No silent writes.
5. **Quality propagates.** A `MeasurementSignal` derived from an OPC UA
   `DataValue` whose `StatusCode` is not `good()` is emitted with
   `Quality.UNCERTAIN` or `Quality.BAD` — never `GOOD`.
6. **Wall-clock timestamps come from the OPC UA server.** Use the
   `sourceTimestamp` from the `DataValue`, falling back to
   `serverTimestamp`, falling back to `System.currentTimeMillis()` only
   if both are null.

## Data flow

```
┌─────────────────────┐    subscriptions     ┌──────────────────────────────┐
│  OPC UA server      │ ───────────────────► │ MiloOpcUaClientService       │
│  (PLC / SCADA /     │                      │  • OpcUaClient + transport   │
│   simulator)        │ ◄─────────────────── │  • OpcUaSubscription         │
└─────────────────────┘    writeValues()     │  • latest-value cache        │
                                             │  • alarm queue               │
                                             └─────┬──────────────┬─────────┘
                                                   │              │
                                  ┌────────────────▼─┐   ┌────────▼───────┐
                                  │ OpcUaMeasurement │   │ OpcUaAlarmInput│
                                  │ Input            │   │                │
                                  │  → IInputSignal  │   │  → IInputSignal│
                                  └────────────────┬─┘   └────────┬───────┘
                                                   │              │
                                                   ▼              ▼
                                  ┌─────────────────────────────────────────┐
                                  │ Existing Jneopallium pipeline:          │
                                  │  Sensor → MeasurementValidator → PID →  │
                                  │  Cascade → MPCPlanning → SafetyGate →   │
                                  │  Actuator                               │
                                  └────────────────┬────────────────────────┘
                                                   │ List<IResult>
                                                   ▼
                                  ┌─────────────────────────────────────────┐
                                  │ OpcUaCommandOutputAggregator            │
                                  │  1. tripped Interlocks → fail-safe write│
                                  │  2. record OperatorOverride entries     │
                                  │  3. for each Setpoint/Actuator command: │
                                  │     INTERLOCK_HOLD?  → reject + audit   │
                                  │     OVERRIDE active? → hold + audit     │
                                  │     SHADOW mode?     → audit only       │
                                  │     ADVISORY mode?   → require execute  │
                                  │     clamp [min,max]                     │
                                  │     rate-limit by rampRateMaxPerSec     │
                                  │     diff-suppress < ε for < 5 s         │
                                  │     writeValues() → audit verdict       │
                                  └────────────────┬────────────────────────┘
                                                   │
                                                   ▼
                                  ┌─────────────────────────────────────────┐
                                  │ OpcUaTransparencyLogOutput              │
                                  │  local JSONL  + optional OPC UA mirror  │
                                  └─────────────────────────────────────────┘
```

## Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/
├── net/neuron/impl/industrial/opcua/
│   ├── OpcUaBridgeConfig.java          ← YAML record schema
│   ├── OpcUaBridgeConfigLoader.java    ← Jackson-YAML loader
│   ├── OpcUaNodeBinding.java           ← runtime binding state
│   ├── OpcUaSignalMapper.java          ← pure mapping functions
│   └── MiloOpcUaClientService.java     ← Milo connection lifecycle
├── net/signals/industrial/opcua/       ← reserved
├── input/opcua/
│   ├── OpcUaMeasurementInput.java      ← IInitInput → MeasurementSignal
│   └── OpcUaAlarmInput.java            ← IInitInput → AlarmSignal
└── output/opcua/
    ├── OpcUaCommandOutputAggregator.java ← safety-critical
    ├── OpcUaTransparencyLogOutput.java   ← append-only audit
    └── OverrideRegistry.java             ← operator override TTL map
```

## Audit JSON schema (one line per record)

```json
{
  "ts": 1740000000000,
  "run": 12345,
  "verdict": "APPLIED" | "REJECTED" | "INTERLOCK_TRIP" | "OVERRIDE_HOLD" | "FAILED",
  "loopId": "FIC-101",
  "tag": "PLANT.FIC101.SP",
  "proposed": 47.3,
  "effective": 45.0,
  "reason": "RATE_LIMITED",
  "safetyMode": "AUTONOMOUS"
}
```

## Configuration

Configuration is YAML, loaded once at startup via
`OpcUaBridgeConfigLoader.load(Path)`. Hot-reload is deliberately not
supported — config changes require a controlled restart of the bridge,
which is the expected industrial workflow (Management of Change).
`FAIL_ON_UNKNOWN_PROPERTIES = true` is enforced; a typo'd field is a
safety incident, not a silent fallback.

See `JNEOPALLIUM_OPCUA_INTEGRATION.md` §4.3 for the canonical example.
