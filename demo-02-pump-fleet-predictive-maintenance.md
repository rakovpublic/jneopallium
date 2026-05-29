# Demo 02 — Pump-fleet predictive maintenance (MQTT + Sparkplug B, ADVISORY)

> Bridge: **MQTT + Sparkplug B** ([doc](../mqtt-bridge.md)) ·
> Module: **[industrial](../modules/industrial.md)** (degradation / maintenance
> layers) · Safety ceiling: **ADVISORY** (structural — the loader rejects
> `AUTONOMOUS`) · External system: any MQTT broker (Mosquitto / EMQX / HiveMQ
> CE).

A read-and-advise demo: a fleet of pumps streams condition data over Sparkplug,
the network estimates remaining useful life (RUL) per asset, and proposes
maintenance windows to the CMMS/HMI — never touching a field actuator.

## Scenario

Twenty centrifugal pumps on a unified-namespace MQTT fabric each publish bearing
**vibration RMS** and **bearing temperature** as Sparkplug metrics. As a bearing
wears, vibration trends up; the network's degradation model converts the trend
into an RUL estimate and, when RUL crosses a horizon, proposes a maintenance
window on an advisory topic that the maintenance scheduler (or a Node-RED/HMI
flow) consumes. A human approves the work order — the bridge is the advisor, not
the actor.

## What it demonstrates

| Feature | Where |
|---|---|
| Slow-loop epochs for slowly-evolving health signals | `DegradationSignal` (2/3), `MaintenanceWindowSignal` (2/10) |
| Per-asset RUL estimation | `DegradationModelNeuron` / `IDegradationModelNeuron` |
| Maintenance window proposal leading predicted end-of-life | `MaintenanceSchedulingNeuron` / `IMaintenanceSchedulingNeuron` |
| ISA-18.2 alarm suppression + rate limiting | `AlarmAggregationNeuron` |
| Structural ADVISORY ceiling (defence-in-depth) | `MqttBridgeConfigLoader` rejects per-tag `AUTONOMOUS` |
| Sparkplug session model & quality propagation | `SparkplugMetricResolver`, `MqttSignalMapper` |
| Bounded outbound advisory queue | `MqttClientService` (`advisoryQueueSize`, R4) |
| Append-only audit + optional MQTT mirror | `MqttAuditOutput` |

## Architecture / data flow

```
 Sparkplug edge nodes (pumps P01..P20)
   spBv1.0/Plant1/DDATA/Edge-Pump/Pxx  { vibration_rms, bearing_temp }
        │ MQTT subscribe (BIRTH cache, quality)
        ▼
  ┌────────────────────────────────────────────┐
  │ MqttClientService  (DefaultMqttTransport,   │
  │  HiveMQ + Eclipse Tahu decode)              │
  └───┬───────────────────────┬─────────────────┘
      ▼                       ▼
 MqttMetricInput          MqttEventInput  (NDEATH/DDEATH → DEVICE_OFFLINE)
   → MeasurementSignal      → AlarmSignal
      │                       │
      ▼                       ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Health sub-net (worker/.../impl/industrial):              │
  │  MeasurementValidator → DegradationModel(per asset)       │
  │       → DegradationSignal(RUL hours, confidence)          │
  │  MaintenanceScheduling → MaintenanceWindowSignal          │
  │  AlarmAggregation (suppress storms, per-minute rate)      │
  │  SafetyGate(mode=ADVISORY)                                │
  └───────────────────────────┬──────────────────────────────┘
                              ▼ List<IResult>
  ┌──────────────────────────────────────────────────────────┐
  │ MqttAdvisoryOutputAggregator (publish-only, clamps)       │
  │  publishes to advisory namespace only:                    │
  │  spBv1.0/Plant1/DCMD/Edge-Pump/Pxx/advisory/maint_window  │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
        Operator HMI / CMMS  +  MqttAuditOutput (JSONL + optional topic)
```

## Components used

* **Signals**: `MeasurementSignal` (1/1, vibration & temp),
  `DegradationSignal` (2/3), `MaintenanceWindowSignal` (2/10), `AlarmSignal`.
* **Neurons**: `MeasurementValidatorNeuron`, `DegradationModelNeuron`,
  `MaintenanceSchedulingNeuron`, `AlarmAggregationNeuron`, `SafetyGateNeuron`
  (ADVISORY).
* **Processors**: `MeasurementValidationProcessor`,
  `DegradationSchedulingProcessor`, `MaintenanceWindowSchedulingProcessor`,
  `AlarmAggregationProcessor`.
* **Bridge** (`worker.bridge.mqtt`): `MqttBridgeConfigLoader`,
  `MqttClientService`, `DefaultMqttTransport`, `SparkplugMetricResolver`,
  `MqttSignalMapper`, `MqttMetricInput`, `MqttEventInput`,
  `MqttAdvisoryOutputAggregator`, `MqttAuditOutput`.

## Configuration

`/tmp/demo02-pumps.yaml`:

```yaml
connection:
  brokerUrl: "tcp://localhost:1883"
  clientId: "jneopallium-pump-fleet"
  cleanSession: false
  keepAlive: "PT30S"
  advisoryQueueSize: 10000
sparkplug:
  enabled: true
  groupId: "Plant1"
  edgeNodeId: "Jneopallium-Reliability"
  advisoryNamespace: "advisory"

reads:
  # one pair of bindings per pump; wildcard segment matches P01..P20
  - bindingId: "PUMP-VIB"
    sparkplugMetric: "Plant1/Edge-Pump/+/vibration_rms"
    signalTag: "PLANT.PUMP.VIB"
  - bindingId: "PUMP-TEMP"
    sparkplugMetric: "Plant1/Edge-Pump/+/bearing_temp"
    signalTag: "PLANT.PUMP.BTEMP"

writes:
  - bindingId: "PUMP-MAINT-ADV"
    advisoryTopic: "spBv1.0/Plant1/DCMD/Edge-Pump/+/advisory/maint_window"
    signalTag:     "PLANT.PUMP.MAINT_WINDOW"
    minClampValue: 0.0
    maxClampValue: 8760.0          # hours-ahead horizon, sanity clamp
    qos: 1

severityMap:
  HIGH_VIB: HIGH
perTagSafetyMode:
  PUMP-MAINT-ADV: ADVISORY         # AUTONOMOUS here is rejected at load time
audit:
  localAuditFile: "/tmp/jneopallium-demo02-audit.jsonl"
  mqttAuditTopic: "spBv1.0/Plant1/DCMD/Edge-Pump/Jneopallium/audit"
  mqttAuditQos: 1
tickInterval: "PT1S"
```

## Run procedure

1. **Start a broker:**

   ```sh
   docker run --rm -p 1883:1883 eclipse-mosquitto:2 \
     mosquitto -c /mosquitto-no-auth.conf
   ```

2. **Emit Sparkplug telemetry.** Use a Tahu sample emitter, a Node-RED flow, or
   a small script that sends a `DBIRTH` then a stream of `DDATA` for
   `Edge-Pump/P01` with a `vibration_rms` metric you can ramp upward over time
   (and a roughly constant `bearing_temp`). Acceptance hinges on a rising
   vibration trend on at least one pump.

3. **Build and wire the bridge:**

   ```java
   var cfg     = MqttBridgeConfigLoader.load(Path.of("/tmp/demo02-pumps.yaml"));
   var audit   = new MqttAuditOutput(Path.of(cfg.audit().localAuditFile()));
   var mapper  = new MqttSignalMapper(cfg);
   var svc     = new MqttClientService(cfg, new DefaultMqttTransport(cfg), mapper, audit);
   svc.start();

   var vibIn   = new MqttMetricInput("mqtt-vib",  svc, List.of("PUMP-VIB"));
   var tempIn  = new MqttMetricInput("mqtt-temp", svc, List.of("PUMP-TEMP"));
   var eventIn = new MqttEventInput("mqtt-events", svc);
   var agg     = new MqttAdvisoryOutputAggregator(svc, audit);
   // build: validator → degradationModel → maintenanceScheduling →
   //        alarmAggregation → safetyGate(ADVISORY)
   ```

4. **Observe RUL fall.** As vibration trends up, the per-asset
   `DegradationSignal.rulHours` should decrease (slow loop, epoch 3 — refreshed
   every 30 ticks). Confidence should rise with sample count.

5. **Observe a window proposal.** When RUL drops below the scheduling horizon,
   `MaintenanceSchedulingNeuron` emits a `MaintenanceWindowSignal` and the
   advisory aggregator publishes a Sparkplug payload to
   `…/Edge-Pump/P01/advisory/maint_window`; an `APPLIED` line lands in the audit
   file. The HMI/CMMS shows a proposed window leading the predicted EOL.

6. **Prove the ceiling.** Edit the config to set
   `perTagSafetyMode.PUMP-MAINT-ADV: AUTONOMOUS` and reload — the loader must
   reject it (advisory is structural for MQTT). Restore `ADVISORY`.

7. **Device-offline alarm.** Send an `NDEATH`/`DDEATH` for `P01`; the bridge
   emits `AlarmSignal(LOW, DEVICE_OFFLINE)` and a `BRIDGE_RECONNECTED` advisory
   on the next reconnect.

## Acceptance

* RUL for the ramped pump decreases monotonically with the vibration trend; flat
  pumps hold steady RUL.
* A maintenance window is proposed only when RUL crosses the horizon, scheduled
  *before* predicted EOL, and is published to the advisory namespace — never to
  a live `DCMD` actuator topic.
* The config loader **rejects** any per-tag `AUTONOMOUS`.
* Alarm storms are suppressed/rate-limited (ISA-18.2) rather than flooding.
* The audit JSONL conforms to `00-FRAMEWORK §4`; the optional MQTT mirror
  carries the same records.

## Safety / regulatory posture

ADVISORY is structural, not a config choice: brokers don't enforce write
authority, delivery is eventually consistent, and Sparkplug quality is
best-effort across implementations — all fine for predictive-maintenance
dashboards, all unsafe for closed-loop control. For any actuating decision, route
through the OPC UA ([demo 01](demo-01-reactor-cascade-control.md)) or PLC4X
bridges. See [`../mqtt-bridge.md`](../mqtt-bridge.md) §"Regulatory posture".
