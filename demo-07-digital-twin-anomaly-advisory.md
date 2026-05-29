# Demo 07 — Digital-twin anomaly advisory (Eclipse Ditto, ADVISORY)

> Bridge: **Eclipse Ditto** ([doc](../ditto-bridge.md)) ·
> Module: **[industrial](../modules/industrial.md)** (efficiency / supervisory
> layers) · Safety ceiling: **ADVISORY** · External system: an **Eclipse Ditto**
> instance (the official sandbox/`docker-compose`) — the bridge also ships an
> in-memory transport for offline runs.

A digital-twin demo. A fleet of HVAC units is represented as Ditto *things*; the
network watches each twin's feature stream, computes a coefficient-of-performance
(COP) efficiency signal against a baseline, and writes an **advisory** feature
back onto the twin (a desired/advisory property) that the facilities dashboard
surfaces. The twin is observed and annotated; the physical actuator is never
driven from here.

## Scenario

Each rooftop HVAC unit has a Ditto thing `org.acme:hvac-<n>` with features
`supplyTemp`, `returnTemp`, `fanRpm`, and `powerKw`. Telemetry updates the twin
via Ditto's Things API. The supervisory sub-net derives COP, compares it to the
unit's rolling baseline, and when efficiency degrades (fouled coil, failing fan)
emits an advisory: it sets `features/advice/properties/recommendedSetpoint` (and
an `anomaly` flag) on the twin. A facilities engineer reviews the advice; the BMS
that actually actuates the unit is untouched by the bridge.

## What it demonstrates

| Feature | Where |
|---|---|
| Ditto feature stream → typed measurements | `DittoFeatureInput`, `DittoSignalMapper`, `DittoFeatureBinding` |
| Twin lifecycle / connectivity events → alarms | `DittoEventInput` |
| Efficiency accounting vs baseline | `EnergyAccountingNeuron` / `IEnergyAccountingNeuron`, `EfficiencySignal` (2/1) |
| Constraint-bounded advisory setpoint nudges | `SetpointOptimiserNeuron` / `ISetpointOptimiserNeuron` |
| Alarm rationalisation | `AlarmAggregationNeuron` |
| Advisory write back to the twin (not the actuator) | `DittoAdvisoryOutputAggregator` |
| ADVISORY ceiling enforced | `DittoBridgeConfigLoader` |
| Append-only audit | `DittoAuditOutput` |

## Architecture / data flow

```
 Eclipse Ditto  (things: org.acme:hvac-1 … hvac-N)
   features/{supplyTemp,returnTemp,fanRpm,powerKw}/properties/value
        │ WebSocket / SSE change events (DefaultDittoTransport)
        ▼
  ┌──────────────────────────────────────────────┐
  │ DittoClientService (DittoTransport)            │
  └───┬───────────────────────────┬───────────────┘
      ▼                           ▼
 DittoFeatureInput            DittoEventInput
   → MeasurementSignal          → AlarmSignal (TWIN_STALE / CONN_LOST)
      │
      ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Supervisory sub-net (worker/.../impl/industrial):         │
  │  MeasurementValidator                                     │
  │  EnergyAccounting → COP vs baseline → EfficiencySignal    │
  │  SetpointOptimiser → bounded advisory setpoint            │
  │  AlarmAggregation (efficiency-drop alarm)                 │
  │  SafetyGate(mode=ADVISORY)                                │
  └───────────────────────────┬──────────────────────────────┘
                              ▼ List<IResult>
  ┌──────────────────────────────────────────────────────────┐
  │ DittoAdvisoryOutputAggregator (advisory only)             │
  │  PATCH features/advice/properties/{recommendedSetpoint,   │
  │  anomaly} on the twin — never a live command channel      │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
        Facilities dashboard (human-in-the-loop)  +  DittoAuditOutput
```

## Components used

* **Signals**: `MeasurementSignal` (twin features, 1/1), `EfficiencySignal`
  (2/1), `SetpointSignal` (advisory, 1/2), `AlarmSignal`.
* **Neurons**: `MeasurementValidatorNeuron`, `EnergyAccountingNeuron`,
  `SetpointOptimiserNeuron`, `AlarmAggregationNeuron`, `SafetyGateNeuron`
  (ADVISORY).
* **Processors**: `MeasurementValidationProcessor`, `EfficiencyOptimiserProcessor`,
  `AlarmAggregationProcessor`.
* **Bridge** (`worker.bridge.ditto`): `DittoBridgeConfigLoader`,
  `DittoClientService`, `DittoTransport` / `DefaultDittoTransport`,
  `DittoSignalMapper`, `DittoFeatureBinding`, `DittoFeatureInput`,
  `DittoEventInput`, `DittoAdvisoryOutputAggregator`, `DittoAuditOutput`.

## Configuration

`/tmp/demo07-twin.yaml`:

```yaml
connection:
  baseUrl: "http://localhost:8080"      # Ditto HTTP/WS gateway
  username: "ditto"
  passwordEnv: "DITTO_PASSWORD"
  transport: "default"                  # or "in-memory" for offline runs

reads:
  - bindingId: "SUPPLY-T"
    thingId: "org.acme:hvac-1"
    featurePath: "features/supplyTemp/properties/value"
    signalTag: "HVAC.1.SUPPLY_T"
  - bindingId: "RETURN-T"
    thingId: "org.acme:hvac-1"
    featurePath: "features/returnTemp/properties/value"
    signalTag: "HVAC.1.RETURN_T"
  - bindingId: "POWER"
    thingId: "org.acme:hvac-1"
    featurePath: "features/powerKw/properties/value"
    signalTag: "HVAC.1.POWER"

efficiency:
  baselineWindowTicks: 2000
  copDropAlarmFraction: 0.15            # 15% below baseline → anomaly

writes:
  - bindingId: "HVAC-ADVICE"
    thingId: "org.acme:hvac-1"
    featurePath: "features/advice/properties/recommendedSetpoint"
    signalTag: "HVAC.1.SETPOINT.ADV"
    minClampValue: 16.0
    maxClampValue: 26.0

perTagSafetyMode:
  HVAC-ADVICE: ADVISORY                  # AUTONOMOUS rejected at load
audit:
  localAuditFile: "/tmp/jneopallium-demo07-audit.jsonl"
tickInterval: "PT1S"
```

## Run procedure

1. **Start Ditto.** Use the Eclipse Ditto `docker-compose` sandbox (gateway on
   `:8080`), or set `transport: in-memory` to run without a server. Create the
   thing and an initial feature set:

   ```sh
   curl -u ditto:ditto -X PUT http://localhost:8080/api/2/things/org.acme:hvac-1 \
     -H 'Content-Type: application/json' \
     -d '{"features":{"supplyTemp":{"properties":{"value":12.0}},
                       "returnTemp":{"properties":{"value":24.0}},
                       "fanRpm":{"properties":{"value":900}},
                       "powerKw":{"properties":{"value":3.2}}}}'
   ```

2. **Build and wire the bridge + supervisory sub-net:**

   ```java
   var cfg    = DittoBridgeConfigLoader.load(Path.of("/tmp/demo07-twin.yaml"));
   var mapper = new DittoSignalMapper(cfg);
   var audit  = new DittoAuditOutput(Path.of(cfg.audit().localAuditFile()));
   var svc    = new DittoClientService(cfg, new DefaultDittoTransport(cfg), mapper, audit);
   svc.start();

   var featIn = new DittoFeatureInput("ditto-feat", svc, List.of("SUPPLY-T","RETURN-T","POWER"));
   var evtIn  = new DittoEventInput("ditto-evt", svc);
   var agg    = new DittoAdvisoryOutputAggregator(svc, audit);
   // build: validator → energyAccounting → setpointOptimiser →
   //        alarmAggregation → safetyGate(ADVISORY)
   ```

3. **Establish a baseline.** Stream healthy telemetry (PATCH the feature values
   periodically). `EnergyAccountingNeuron` anchors a COP baseline; confirm
   `EfficiencySignal` sits near 1.0 of baseline and no advice is written.

4. **Degrade efficiency.** PATCH telemetry to simulate fouling — same `powerKw`
   but a smaller `supplyTemp`/`returnTemp` delta (lower useful cooling). COP
   drops; once it crosses `copDropAlarmFraction`, an efficiency alarm fires and
   `SetpointOptimiser` proposes a bounded setpoint nudge.

5. **Observe the advisory write.** The aggregator PATCHes
   `features/advice/properties/recommendedSetpoint` (clamped to `[16,26]`) and an
   `anomaly` flag on the twin; an `APPLIED` audit line is written. The dashboard
   shows the advice on the twin — no command channel is touched.

6. **Confirm the ceiling.** Set `perTagSafetyMode: AUTONOMOUS` and reload — the
   loader must reject it.

## Acceptance

* Healthy telemetry → efficiency near baseline, no advice written.
* A simulated COP drop raises an efficiency alarm and writes a clamped advisory
  setpoint plus an anomaly flag onto the twin's `advice` feature.
* The bridge only ever PATCHes the advisory feature path — never a command/desired
  channel wired to a physical actuator.
* `AUTONOMOUS` is rejected; the audit JSONL conforms to `00-FRAMEWORK §4`.

## Safety / regulatory posture

Digital twins are an eventually-consistent representation, not a real-time
control surface; the bridge therefore writes **advice onto the twin** for a human
/ BMS to act on, and the ceiling is ADVISORY. For closed-loop actuation, the
physical device should be driven through OPC UA / PLC4X
([demo 01](demo-01-reactor-cascade-control.md)) with the twin kept as the
observability and advisory layer. See [`../ditto-bridge.md`](../ditto-bridge.md).
