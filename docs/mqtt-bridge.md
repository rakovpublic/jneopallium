# MQTT + Sparkplug B bridge

> Companion to [`02-MQTT-SPARKPLUG.md`](../02-MQTT-SPARKPLUG.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This file
> documents the MQTT/Sparkplug bridge's domain context, YAML schema, manual
> demo procedure, and regulatory posture — i.e. the §10 Definition-of-Done
> items that don't belong in the spec itself.

## Domain context

[MQTT](https://mqtt.org/) is the dominant publish/subscribe protocol for
industrial IoT — lightweight, broker-mediated, and well-supported on edge
gateways. [Sparkplug B](https://sparkplug.eclipse.org/) is the Eclipse
specification that imposes a topic structure
(`spBv1.0/<group>/<msg-type>/<edge>/<device>`), a Protobuf payload schema with
rich-typed metrics, and a session-state model
(`NBIRTH`/`NDEATH`/`DBIRTH`/`DDEATH`/`NDATA`/`DDATA`/`NCMD`/`DCMD`) on top of
MQTT. Sparkplug is what most modern unified-namespace deployments standardise
on; plain MQTT is supported here as a fallback for greenfield smart-factory
and home-IoT devices that don't speak Sparkplug.

The bridge is positioned as **read + advisory write only**. Brokered,
eventually-consistent, multi-client topics are the wrong substrate for
safety-critical control writes; if you need autonomous writes, route them
through the OPC UA or PLC4X bridges instead. The aggregator never publishes
to a `DCMD` topic that triggers a real field actuator — it publishes to a
configurable advisory namespace
(`spBv1.0/<group>/DCMD/<edge>/<device>/advisory/...`) consumed by the operator
HMI. The HMI is the human-in-the-loop. AUTONOMOUS per-tag promotion is rejected
by the config validator, defence-in-depth.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/mqtt/
├── MqttBridgeConfig.java               ← YAML record (immutable, AUTONOMOUS rejected)
├── MqttBridgeConfigLoader.java         ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── MqttTopicBinding.java               ← read/write binding (BridgeBinding)
├── SparkplugMetricResolver.java        ← spBv1.0 topic + metric → bindingId
├── MqttSignalMapper.java               ← Sparkplug + plain-JSON → typed signal
├── MqttTransport.java                  ← test-seam interface
├── DefaultMqttTransport.java           ← HiveMQ-backed production wiring
├── MqttClientService.java              ← lifecycle, BIRTH cache, advisory queue
├── MqttMetricInput.java                ← IInitInput → MeasurementSignal
├── MqttEventInput.java                 ← IInitInput → AlarmSignal (DEVICE_OFFLINE)
├── MqttAuditOutput.java                ← local JSONL audit + optional MQTT mirror
├── MqttAdvisoryOutputAggregator.java   ← IOutputAggregator (publish-only, with clamps)
└── package-info.java
```

The bridge core depends on Eclipse Tahu for Sparkplug B
encode/decode and the HiveMQ client for the MQTT transport. Tests inject an
in-memory `MqttTransport` (`InMemoryMqttTransport` in the test tree), so
acceptance scenarios run without a broker.

## YAML schema reference

```yaml
connection:
  brokerUrl: "ssl://broker.plant.local:8883"
  clientId: "jneopallium-bridge-edge01"
  cleanSession: false
  keepAlive: "PT30S"
  advisoryQueueSize: 10000               # bound on outbound queue (R4)

security:
  type: "UsernamePassword"               # None | UsernamePassword | ClientCertificate
  username: "jneopallium"
  passwordEnv: "MQTT_PASSWORD"           # env-var name; never embed the secret
  trustStore: "/etc/jneopallium/mqtt-truststore.jks"

sparkplug:
  enabled: true
  groupId: "Plant1"
  edgeNodeId: "Jneopallium-Edge-01"
  advisoryNamespace: "advisory"          # default — reserved part of the DCMD path

reads:
  - bindingId: "TIC-101"
    sparkplugMetric: "Plant1/Edge-Reactor/Reactor1/temperature"
    signalTag: "PLANT.TIC101.PV"
    signalKind: "MEASUREMENT"            # default

  - bindingId: "AMBIENT-TEMP"            # plain-MQTT path
    plainMqttTopic: "sensors/ambient/temperature"
    jsonPath: "$.value"                  # required for plain MQTT measurements
    signalTag: "FACILITY.AMBIENT.PV"

writes:
  - bindingId: "TIC-101-ADV"
    advisoryTopic:  "spBv1.0/Plant1/DCMD/Edge-Reactor/Reactor1/advisory/setpoint_temperature"
    signalTag:      "PLANT.TIC101.SP"
    sparkplugMetric: "Plant1/Edge-Reactor/Reactor1/setpoint_temperature"
    minClampValue:  0.0
    maxClampValue: 100.0
    qos: 1

audit:
  localAuditFile: "/var/log/jneopallium/mqtt-audit.jsonl"
  mqttAuditTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Jneopallium/audit"
  mqttAuditQos:    1

severityMap:
  HIGH_TEMP: HIGH                        # Sparkplug alarm metric → AlarmPriority

perTagSafetyMode:
  TIC-101-ADV: ADVISORY                  # AUTONOMOUS rejected by the loader

tickInterval: "PT0.25S"
```

`reads` accept exactly one of `sparkplugMetric` or `plainMqttTopic`. Plain-MQTT
measurement bindings must declare a `jsonPath`. Wildcard Sparkplug addresses
(`Plant1/Edge-Pump/+/temperature_*`) are supported and matched per segment.

## Manual demo procedure

Pre-requisites: Java 17, Maven 3.9+, an MQTT broker reachable from the host
(Mosquitto, EMQX, or HiveMQ Community Edition).

1. Start a broker:

   ```sh
   docker run --rm -p 1883:1883 eclipse-mosquitto:2 mosquitto -c /mosquitto-no-auth.conf
   ```

2. Save a config to `/tmp/mqtt-demo.yaml`:

   ```yaml
   connection:
     brokerUrl: "tcp://localhost:1883"
     clientId: "jneopallium-demo"
   sparkplug:
     enabled: true
     groupId:    "Plant1"
     edgeNodeId: "Jneopallium-Demo"
   reads:
     - bindingId: "TIC-101"
       sparkplugMetric: "Plant1/Edge-Reactor/Reactor1/temperature"
       signalTag: "PLANT.TIC101.PV"
   writes:
     - bindingId: "TIC-101-ADV"
       advisoryTopic: "spBv1.0/Plant1/DCMD/Edge-Reactor/Reactor1/advisory/setpoint_temperature"
       signalTag:     "PLANT.TIC101.SP"
       minClampValue:  0.0
       maxClampValue: 100.0
   audit:
     localAuditFile: "/tmp/mqtt-audit.jsonl"
   perTagSafetyMode:
     TIC-101-ADV: ADVISORY
   ```

3. Construct the bridge and inject inputs/outputs:

   ```java
   MqttBridgeConfig cfg = MqttBridgeConfigLoader.load(Path.of("/tmp/mqtt-demo.yaml"));
   MqttAuditOutput  audit = new MqttAuditOutput(Path.of(cfg.audit().localAuditFile()));
   MqttSignalMapper mapper = new MqttSignalMapper(cfg);
   MqttTransport transport = new DefaultMqttTransport(cfg);
   MqttClientService svc = new MqttClientService(cfg, transport, mapper, audit);
   svc.start();

   MqttMetricInput              metricIn = new MqttMetricInput("mqtt-meas", svc, List.of("TIC-101"));
   MqttEventInput               eventIn  = new MqttEventInput("mqtt-events", svc);
   MqttAdvisoryOutputAggregator agg      = new MqttAdvisoryOutputAggregator(svc, audit);
   ```

4. Publish a Sparkplug DBIRTH then a DDATA from any Sparkplug emitter (a Tahu
   sample app, a Node-RED flow, or a direct `mosquitto_pub` carrying a
   Tahu-encoded Protobuf payload) and observe one `MeasurementSignal` per
   tick draining out of the metric input. Disconnect the device; the bridge
   emits `AlarmSignal(LOW, DEVICE_OFFLINE)` plus a `BRIDGE_RECONNECTED` advisory
   on next reconnect.

5. Drive a `SetpointSignal(tag="PLANT.TIC101.SP", setpoint=72.5)` into the
   aggregator. The advisory topic receives a Sparkplug payload carrying the
   clamped value; an APPLIED audit line is written to
   `/tmp/mqtt-audit.jsonl`.

## Regulatory posture

The structural ceiling is **ADVISORY**, both in spec and in code: the config
loader rejects any per-tag `AUTONOMOUS` promotion. This is appropriate
because:

* MQTT brokers do not enforce write authority; any client with credentials
  can publish to the topic the bridge writes to. The bridge audits its own
  writes but cannot prevent a misconfigured shadow client from publishing in
  parallel.
* Eventual consistency: a subscriber may not receive the latest setpoint for
  hundreds of milliseconds under load — fine for advisory dashboards and
  predictive maintenance, unsafe for closed-loop control.
* Sparkplug's `is_null`/quality propagation is best-effort across
  implementations; the bridge propagates what is present (per
  framework §0.5) but cannot guarantee a globally consistent quality model.

For closed-loop control of field actuators, route through the OPC UA or
PLC4X bridges, both of which are AUTONOMOUS-capable per-loop with the full
clamp / rate-limit / interlock / override ladder enforced before any write.

The local JSONL audit conforms to `00-FRAMEWORK §4`; an optional
`mqttAuditTopic` mirror publishes the same record to a SOC-bound MQTT
channel and is best-effort (`audit.mqttAuditTopic` config field).
