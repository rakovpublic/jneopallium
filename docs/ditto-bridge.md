# Eclipse Ditto bridge

> Companion to [`10-DITTO.md`](../10-DITTO.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This file
> documents the Ditto bridge's domain context, YAML schema, manual demo
> procedure, and regulatory posture — i.e. the §10 Definition-of-Done items
> that don't belong in the spec itself.

## Domain context

[Eclipse Ditto](https://www.eclipse.org/ditto/) is the open-source framework
for managing **digital twins** of internet-connected devices. A *Thing* in
Ditto carries features (state slots, e.g. `vibration`, `temperature`),
attributes (static metadata), and a policy (access control). Devices push
state in; consumers read it via REST or subscribe to twin events over a
WebSocket; controllers write commands addressed to twin features.

Ditto sits one layer above raw protocol bridges (MQTT, OPC UA, PLC4X). Where
those bridges expose *measurement points*, Ditto exposes a *coherent device
model*: "Pump-3 has a vibration feature that is currently 4.2 mm/s and was
last calibrated on 2025-07-01." For Jneopallium that's useful when reasoning
needs **device context** more than raw signal density — predictive
maintenance, fleet-level anomaly detection, energy optimization across
heterogeneous equipment.

The bridge is positioned as **read + advisory write only**. It writes
exclusively to feature names prefixed with `recommended_` or `advisory_` —
never to the actual control feature. That rule is enforced *twice*: at config
load (the loader rejects any write binding whose feature lacks the prefix)
and at runtime in `DittoClientService.writeProperty` (defence-in-depth, per
10-DITTO §4). `AUTONOMOUS` per-tag promotion is also rejected by the config
validator: the structural ceiling is `ADVISORY`.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/ditto/
├── DittoBridgeConfig.java                ← YAML record (immutable, AUTONOMOUS rejected)
├── DittoBridgeConfigLoader.java          ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── DittoFeatureBinding.java              ← thingId/feature/property binding (BridgeBinding)
├── DittoSignalMapper.java                ← twin event JSON ↔ typed signal / command body
├── DittoTransport.java                   ← test-seam interface (REST + WS)
├── DefaultDittoTransport.java            ← java.net.http-backed production wiring
├── DittoClientService.java               ← lifecycle, alive-thing cache, advisory queue
├── DittoFeatureInput.java                ← IInitInput → MeasurementSignal
├── DittoEventInput.java                  ← IInitInput → AlarmSignal (TWIN_OFFLINE / RECONNECTED)
├── DittoAuditOutput.java                 ← local JSONL audit (00-FRAMEWORK §4)
├── DittoAdvisoryOutputAggregator.java    ← IOutputAggregator (writes only to advisory features)
└── package-info.java
```

The production transport uses the JDK's built-in `HttpClient` and `WebSocket`
client to speak the [Ditto
protocol](https://www.eclipse.org/ditto/protocol-overview.html) directly,
which keeps the dependency surface minimal — no Akka classic transitive deps
from `org.eclipse.ditto:ditto-client`. Tests inject an in-memory
`DittoTransport` (`InMemoryDittoTransport` in the test tree), so acceptance
scenarios run without a Ditto sandbox. Deployments that want richer
protocol features (search, live channels) can swap in a transport implementing
the same seam.

## YAML schema reference

```yaml
connection:
  baseUrl: "https://ditto.factory.local"
  webSocketPath: "/ws/2"                  # default
  httpPath: "/api/2"                      # default
  requestTimeout: "PT10S"                 # default
  advisoryQueueSize: 10000                # bound on outbound queue

authentication:
  type: "OAuth2BearerToken"               # None | BasicAuth | OAuth2BearerToken
  tokenEndpoint: "https://idp.local/token"
  clientId: "jneopallium-bridge"
  clientSecretEnv: "DITTO_CLIENT_SECRET"  # env-var name; never embed the secret

# Optional explicit list of subscribed things. The bridge automatically
# subscribes to any thing referenced from a read or write binding too.
things:
  - "factory.line-a:pump-1"
  - "factory.line-a:reactor-1"

reads:
  - bindingId: "PUMP1-VIB"
    thingId:   "factory.line-a:pump-1"
    feature:   "vibration"
    property:  "rms_z"
    signalTag: "PUMP01.VIB.Z"
    signalKind: "MEASUREMENT"             # default; or ALARM for booleans

  - bindingId: "REACTOR1-TEMP"
    thingId:   "factory.line-a:reactor-1"
    feature:   "temperature"
    property:  "current"
    signalTag: "REACTOR01.TEMP"

writes:
  - bindingId: "REACTOR1-ADVISED-SP"
    thingId:   "factory.line-a:reactor-1"
    feature:   "recommended_setpoint"     # MUST start with "recommended_" or "advisory_"
    property:  "value"
    signalTag: "REACTOR01.SP.ADV"
    minClampValue:  0.0
    maxClampValue: 100.0

audit:
  localAuditFile: "/var/log/jneopallium/ditto-audit.jsonl"

severityMap:
  high_temp: HIGH                         # boolean alarm feature → AlarmPriority

perTagSafetyMode:
  REACTOR1-ADVISED-SP: ADVISORY           # AUTONOMOUS rejected by the loader

tickInterval: "PT0.25S"
```

`reads` produce a `MeasurementSignal` (numeric property) or an `AlarmSignal`
(boolean property whose feature name appears in `severityMap`). Quality
defaults to `GOOD`; when the bridge has flagged the thing as offline (after a
`THING_DELETED` event) subsequent reads carry `Quality.UNCERTAIN` per
00-FRAMEWORK §0.5.

`writes` are restricted to features whose name starts with `recommended_` or
`advisory_`. The compact constructor of `WriteBindingConfig` throws
`IllegalArgumentException` on any other value — Jackson surfaces it as a
load failure.

## Manual demo procedure

Pre-requisites: Java 17, Maven 3.9+, Docker, and a clone of
[`eclipse-ditto/ditto-examples`](https://github.com/eclipse-ditto/ditto-examples).

1. Start a local Ditto sandbox:

   ```sh
   git clone https://github.com/eclipse-ditto/ditto.git
   cd ditto/deployment/docker
   docker compose up -d
   ```

2. Register a thing (REST):

   ```sh
   curl -u ditto:ditto -X PUT \
     "http://localhost:8080/api/2/things/factory.line-a:reactor-1" \
     -H 'Content-Type: application/json' \
     -d '{"features":{"temperature":{"properties":{"current":25.0}},
                       "recommended_setpoint":{"properties":{"value":0.0}}}}'
   ```

3. Save a config to `/tmp/ditto-demo.yaml`:

   ```yaml
   connection:
     baseUrl: "http://localhost:8080"
   authentication:
     type: "BasicAuth"
     username: "ditto"
     passwordEnv: "DITTO_DEMO_PWD"
   reads:
     - bindingId: "REACTOR1-TEMP"
       thingId:   "factory.line-a:reactor-1"
       feature:   "temperature"
       property:  "current"
       signalTag: "REACTOR01.TEMP"
   writes:
     - bindingId: "REACTOR1-ADVISED-SP"
       thingId:   "factory.line-a:reactor-1"
       feature:   "recommended_setpoint"
       property:  "value"
       signalTag: "REACTOR01.SP.ADV"
       minClampValue:  0.0
       maxClampValue: 100.0
   audit:
     localAuditFile: "/tmp/ditto-audit.jsonl"
   perTagSafetyMode:
     REACTOR1-ADVISED-SP: ADVISORY
   ```

   ```sh
   export DITTO_DEMO_PWD=ditto
   ```

4. Construct the bridge and inject inputs/outputs:

   ```java
   DittoBridgeConfig cfg = DittoBridgeConfigLoader.load(Path.of("/tmp/ditto-demo.yaml"));
   DittoAuditOutput  audit = new DittoAuditOutput(Path.of(cfg.audit().localAuditFile()));
   DittoSignalMapper mapper = new DittoSignalMapper(cfg);
   DittoTransport transport = new DefaultDittoTransport(cfg);
   DittoClientService svc = new DittoClientService(cfg, transport, mapper, audit);
   svc.start();

   DittoFeatureInput               featureIn = new DittoFeatureInput("ditto-meas", svc, List.of("REACTOR1-TEMP"));
   DittoEventInput                 eventIn   = new DittoEventInput("ditto-events", svc);
   DittoAdvisoryOutputAggregator   agg       = new DittoAdvisoryOutputAggregator(svc, audit);
   ```

5. Update the source feature property via REST and observe one
   `MeasurementSignal` per tick draining out of `featureIn`:

   ```sh
   curl -u ditto:ditto -X PUT \
     "http://localhost:8080/api/2/things/factory.line-a:reactor-1/features/temperature/properties/current" \
     -H 'Content-Type: application/json' -d '73.5'
   ```

6. Drive a `SetpointSignal(tag="REACTOR01.SP.ADV", setpoint=72.5)` into the
   aggregator. The bridge issues a `PUT` to
   `/api/2/things/factory.line-a:reactor-1/features/recommended_setpoint/properties/value`
   with the clamped scalar payload, and writes an `APPLIED` audit line to
   `/tmp/ditto-audit.jsonl`.

7. Delete the thing (`curl -u ditto:ditto -X DELETE …`) and observe the
   bridge emit `AlarmSignal(HIGH, TWIN_OFFLINE)` plus subsequent reads
   carrying `Quality.UNCERTAIN`.

## Regulatory posture

The structural ceiling is **ADVISORY**, both in spec and in code:

* The config loader rejects any per-tag `AUTONOMOUS` promotion.
* The config loader rejects any write binding whose feature name does not
  start with `recommended_` or `advisory_`. Re-checked at runtime in
  `DittoClientService.writeProperty` for defence-in-depth — a programmatic
  caller can't bypass it by hand-rolling a binding either.

This is appropriate because:

* Ditto is a context model, not a control plane. Closed-loop control through
  a digital-twin API would route through Ditto's connectivity service to a
  protocol bridge anyway — better to advise the operator HMI directly.
* Policy-based access denial mid-run can occur silently from the bridge's
  perspective; a write that a Ditto policy refuses produces an HTTP 403 and
  is audited as `FAILED`. The bridge does not infer values from a 403; the
  affected binding stays at the last known value with `Quality.UNCERTAIN`
  on subsequent reads (per 10-DITTO §10 R3, 00-FRAMEWORK §0.5).
* Per-binding feature path is validated at config load (10-DITTO §10 R1).

For closed-loop control of field actuators, route through the OPC UA or
PLC4X bridges, both of which are AUTONOMOUS-capable per-loop with the full
clamp / rate-limit / interlock / override ladder enforced before any write.

The local JSONL audit conforms to `00-FRAMEWORK §4`. There is no Ditto-side
audit mirror channel by default — the source REST API is itself the audit
trail of writes the bridge has issued.
