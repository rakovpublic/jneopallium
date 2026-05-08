# Bridge 02 — MQTT + Sparkplug B (industrial IoT / unified namespace)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** very high. **Safety ceiling:** `ADVISORY` (no autonomous write to field actuators by design — see §3).

## 1. Domain context

[MQTT](https://mqtt.org/) is the dominant publish/subscribe protocol for IIoT — lightweight, broker-mediated, well-supported on edge gateways. [Sparkplug B](https://sparkplug.eclipse.org/) is the Eclipse specification that imposes a topic structure (`spBv1.0/<group>/<message-type>/<edge>/<device>`), a payload schema (Google Protobuf with rich-typed metrics), and a session-state model (`NBIRTH`/`NDEATH`/`DBIRTH`/`DDEATH`/`NDATA`/`DDATA`/`NCMD`/`DCMD`) on top of MQTT.

Sparkplug is what most modern unified-namespace deployments standardise on. Plain MQTT is supported as a fallback for greenfield smart-factory and home-IoT devices that don't speak Sparkplug.

The MQTT/Sparkplug bridge is positioned as **read + advisory write only**. Brokered, eventually-consistent, multi-client topics are the wrong substrate for safety-critical control writes. If you need autonomous writes, route them through OPC UA or PLC4X instead.

## 2. Maven dependencies

```xml
<!-- HiveMQ MQTT client — production-grade, java 8+, java records-friendly -->
<dependency>
    <groupId>com.hivemq</groupId>
    <artifactId>hivemq-mqtt-client</artifactId>
    <version>1.3.3</version>
</dependency>
<!-- Eclipse Tahu — official Sparkplug B Java implementation -->
<dependency>
    <groupId>org.eclipse.tahu</groupId>
    <artifactId>tahu-core</artifactId>
    <version>1.0.7</version>
</dependency>
```

Verify latest before merge.

## 3. Why advisory only

Three reasons, in declining order of severity:

1. **Brokers do not enforce write authority.** Any client with credentials can publish a `DCMD`. The aggregator can audit its own writes but cannot prevent a misconfigured shadow client from also writing.
2. **Eventual consistency.** A subscriber may not receive the latest setpoint for hundreds of milliseconds under load. This is acceptable for advisory dashboards and predictive maintenance, unacceptable for closed-loop control.
3. **No standardised quality field.** Sparkplug metrics carry an `is_null` and a `quality` extension property in some implementations, but the convention is not universally honoured. Quality propagation per framework §0.5 is best-effort.

The aggregator therefore **never** publishes to a `DCMD` topic that triggers a field actuator. It publishes only to advisory topics under a configurable advisory namespace (e.g. `spBv1.0/<group>/DCMD/<edge>/jneopallium/advisory/...`) that the operator HMI subscribes to. The HMI is the human-in-the-loop.

## 4. Architecture

```
┌──────────────────────┐    SUBSCRIBE             ┌────────────────────────┐
│  MQTT broker         │ ───────────────────────▶ │ MqttClientService      │
│  (HiveMQ / EMQX /    │                          │  • HiveMQ client       │
│   Mosquitto)         │ ◀─── PUBLISH (advisory)  │  • Sparkplug session   │
└──────────────────────┘                          │  • metric cache by     │
                                                  │    group/edge/device   │
                                                  │    /metric             │
                                                  └─────┬───────────┬──────┘
                                                        │           │
                                                ┌───────▼─┐  ┌──────▼──────┐
                                                │ MqttMet │  │ MqttEvent   │
                                                │ ricInput│  │ Input       │
                                                │  → Mea- │  │  → Alarm-   │
                                                │  surement│  │    Signal   │
                                                └──────────┘  └─────────────┘
                                                              ▼
                                  [Pipeline → MqttAdvisoryOutputAggregator]
                                                              ▼
                                                  PUBLISH to advisory topics only
```

## 5. Signal mapping

| MQTT/Sparkplug ingress | Jneopallium signal | Notes |
|---|---|---|
| Sparkplug `NDATA`/`DDATA` numeric metric | `MeasurementSignal` | `signalTag = group/edge/device/metric`. Quality from Sparkplug `is_historical`/`is_transient` extensions. |
| Sparkplug `DBIRTH` | bridge state event (no signal) | Used to refresh the cached metric metadata table for a device. |
| Sparkplug `DDEATH`/`NDEATH` | `AlarmSignal(LOW, "DEVICE_OFFLINE")` | Plus all metrics from that device flagged `Quality.UNCERTAIN` until next BIRTH. |
| Plain MQTT JSON message | `MeasurementSignal` (mapped via per-topic `jsonPath` config) | Quality `GOOD` unless the message itself carries a quality field. |
| Sparkplug alarm metric (boolean tagged) | `AlarmSignal` | Severity from a `severityMap` in YAML, like PLC4X. |

Egress (advisory):

| Jneopallium signal | MQTT publish | Notes |
|---|---|---|
| `SetpointSignal` | Sparkplug `DCMD` to advisory topic | Operator HMI consumes; no actuation. |
| `ActuatorCommandSignal` | Same advisory topic with `target` payload | Same. |
| `TransparencyLogSignal` | Optional dedicated audit topic | If `audit.mqttAuditTopic` is configured. |

## 6. Configuration

```yaml
connection:
  brokerUrl: "ssl://broker.plant.local:8883"
  clientId: "jneopallium-bridge-edge01"
  cleanSession: false
  keepAlive: "PT30S"

security:
  type: "UsernamePassword"
  username: "jneopallium"
  passwordEnv: "MQTT_PASSWORD"
  trustStore: "/etc/jneopallium/mqtt-truststore.jks"

sparkplug:
  enabled: true
  groupId: "Plant1"
  edgeNodeId: "Jneopallium-Edge-01"

reads:
  - bindingId: "TIC-101"
    sparkplugMetric: "Plant1/Edge-Reactor/Reactor1/temperature"
    signalTag: "PLANT.TIC101.PV"

  - bindingId: "VIBRATION-Z"
    sparkplugMetric: "Plant1/Edge-Pump/Pump3/vibration_z"
    signalTag: "PLANT.PUMP3.VIB_Z"

  # Plain MQTT path (Sparkplug not used)
  - bindingId: "AMBIENT-TEMP"
    plainMqttTopic: "sensors/ambient/temperature"
    jsonPath: "$.value"
    signalTag: "FACILITY.AMBIENT.PV"

writes:
  - bindingId: "TIC-101-ADV"
    advisoryTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Reactor1/advisory/setpoint_temperature"
    signalTag: "PLANT.TIC101.SP"
    minClampValue: 0.0
    maxClampValue: 100.0

audit:
  localAuditFile: "/var/log/jneopallium/mqtt-audit.jsonl"
  mqttAuditTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Jneopallium/audit"

perTagSafetyMode:
  TIC-101-ADV: ADVISORY    # AUTONOMOUS is rejected by config validator for this bridge
```

The config loader **rejects** `AUTONOMOUS` mode for any binding under this bridge — the structural ceiling is enforced in code.

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/mqtt/
├── MqttBridgeConfig.java
├── MqttBridgeConfigLoader.java
├── MqttTopicBinding.java
├── SparkplugMetricResolver.java
├── MqttSignalMapper.java
├── MqttClientService.java
├── MqttMetricInput.java
├── MqttEventInput.java
└── MqttAdvisoryOutputAggregator.java
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Read-only Sparkplug subscribe; map `MeasurementSignal` and `AlarmSignal`; survive a broker restart per S4. |
| 2 | Advisory publish to a dedicated advisory namespace; HMI integration is out of scope but the topic schema is documented. |
| 3 | **Not applicable.** Autonomous is permanently disabled for this bridge. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | DBIRTH refresh | Edge node restarts and resends DBIRTH | All cached metrics for that device update their type metadata; first DDATA after birth produces signals as normal |
| **S8** | DDEATH propagation | Device disconnects | All metrics of that device emit `Quality.UNCERTAIN` on next read tick; one `AlarmSignal(LOW, DEVICE_OFFLINE)` emitted |
| **S9** | AUTONOMOUS rejected at config | YAML sets `perTagSafetyMode: { X: AUTONOMOUS }` | `MqttBridgeConfigLoader.load()` throws with message naming the bridge ceiling |
| **S10** | QoS 0 dropped messages | Simulate broker dropping a metric | No invented values; `MeasurementSignal` simply absent for that tick |
| **S11** | Plain MQTT JSONPath | Topic delivers `{"value": 23.5, "ts": "2026-05-08T12:00:00Z"}` | `MeasurementSignal.value=23.5`, `timestamp` parsed from `ts` field per framework §0.6 |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Broker is the entire trust boundary | Document MQTT TLS + client cert requirements in production. Spec mandates `ssl://` in the production runbook. |
| R2 | Sparkplug spec changes between minor versions | Pin Tahu version; treat the spec as a frozen interface. |
| R3 | Topic explosion (one bridge subscribed to thousands of metrics) | Bridge supports topic-pattern subscriptions (`spBv1.0/Plant1/DDATA/+/+/temperature_*`) with regex post-filter; document upper bound in config validator. |
| R4 | Backpressure when broker faster than network can publish advisory | Advisory publish queue is bounded; on overflow, oldest advisory is dropped and an audit `verdict=FAILED reason=ADVISORY_QUEUE_FULL` recorded. |

## 11. References

* Sparkplug B specification — `https://sparkplug.eclipse.org/specification/`.
* Eclipse Tahu Java reference — `https://github.com/eclipse-tahu/tahu`.
* HiveMQ MQTT client docs — `https://github.com/hivemq/hivemq-mqtt-client`.
