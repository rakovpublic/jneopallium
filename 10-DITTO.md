# Bridge 10 — Eclipse Ditto (digital twin API bridge)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** medium-high. **Safety ceiling:** `ADVISORY`.

## 1. Domain context

[Eclipse Ditto](https://www.eclipse.org/ditto/) is an open-source framework for managing **digital twins** of internet-connected devices. A *Thing* in Ditto has features (state slots), attributes (static metadata), and a policy (access control). Devices push state in; consumers pull state out via REST or WebSocket; messages can be addressed to twin commands.

Ditto sits one layer above raw protocol bridges (MQTT, OPC UA, PLC4X). Where those bridges expose *measurement points*, Ditto exposes a *coherent device model*: "Pump-3 has a vibration feature that is currently 4.2 mm/s and was last calibrated on 2025-07-01."

For Jneopallium this is useful when reasoning needs **device context** more than raw signal density — predictive maintenance, fleet-level anomaly detection, energy optimization across heterogeneous gear.

## 2. Maven dependencies

```xml
<!-- Eclipse Ditto Java client -->
<dependency>
    <groupId>org.eclipse.ditto</groupId>
    <artifactId>ditto-client</artifactId>
    <version>3.6.0</version>
</dependency>
<dependency>
    <groupId>org.eclipse.ditto</groupId>
    <artifactId>ditto-json</artifactId>
    <version>3.6.0</version>
</dependency>
```

Verify the latest before merge.

## 3. Architecture

```
┌──────────────────────┐                          ┌────────────────────────┐
│ Eclipse Ditto        │   live thing/feature     │ DittoClientService     │
│   - Things service   │       updates            │  • DittoClient (WS)    │
│   - Connectivity     │ ───────────────────────▶ │  • Twin event listeners│
│   - Policies         │                          │  • Per-thing cache     │
│                      │ ◀── twin command (advis) │  • Reconnect           │
└──────────────────────┘                          └─────┬───────────┬──────┘
                                                        │           │
                                                ┌───────▼─┐  ┌──────▼──────┐
                                                │ DittoFea│  │ DittoEvent  │
                                                │ tureInpu│  │ Input       │
                                                │ → Mea-  │  │ → Alarm     │
                                                │   sure- │  │   (twin     │
                                                │   ment  │  │    error)   │
                                                └─────────┘  └─────────────┘
                                                              ▼
                                  [Pipeline → DittoAdvisoryOutputAggregator]
                                                              ▼
                                  twin command to advisory feature only
                                  (e.g., feature `recommended_setpoint`)
```

## 4. Signal mapping — reuse industrial signals

| Ditto thing/feature | Jneopallium signal | Notes |
|---|---|---|
| feature property (numeric) | `MeasurementSignal` | `signalTag = thingId/feature/property`. Quality `GOOD` unless feature carries a quality field. |
| feature property (boolean alarm) | `AlarmSignal` | Severity from a `severityMap` block. |
| feature definition (static) | bridge metadata | Used for tag prefix conventions and alarm enrichment. |
| thing-deleted / connection-lost twin event | `AlarmSignal(MEDIUM, "TWIN_OFFLINE")` | Plus all features of that thing flagged `Quality.UNCERTAIN`. |

Egress (advisory):

| Jneopallium signal | Ditto write | Notes |
|---|---|---|
| `SetpointSignal` | twin command updating `recommended_<x>` feature on the same thing | The actual control feature is **not** written. |
| `MaintenanceWindowSignal` | twin command on a `maintenance_advisory` feature | Operator queue, not direct scheduling. |

The aggregator forbids any write to a feature whose name does not start with `recommended_` or `advisory_`. Enforced at config-load and at runtime.

## 5. Configuration

```yaml
ditto:
  baseUrl: "https://ditto.factory.local"
  authentication:
    type: "OAuth2BearerToken"
    tokenEndpoint: "..."
    clientId: "jneopallium-bridge"
    clientSecretEnv: "DITTO_CLIENT_SECRET"
  # Ditto's namespaces
  things:
    - "factory.line-a:pump-1"
    - "factory.line-a:pump-2"
    - "factory.line-a:reactor-1"

reads:
  - bindingId: "PUMP1-VIB"
    thingId: "factory.line-a:pump-1"
    feature: "vibration"
    property: "rms_z"
    signalTag: "PUMP01.VIB.Z"

  - bindingId: "REACTOR1-TEMP"
    thingId: "factory.line-a:reactor-1"
    feature: "temperature"
    property: "current"
    signalTag: "REACTOR01.TEMP"

writes:
  - bindingId: "REACTOR1-ADVISED-SP"
    thingId: "factory.line-a:reactor-1"
    feature: "recommended_setpoint"     # MUST start with "recommended_" or "advisory_"
    property: "value"
    signalTag: "REACTOR01.SP.ADV"

audit:
  localAuditFile: "/var/log/jneopallium/ditto-audit.jsonl"

perTagSafetyMode:
  REACTOR1-ADVISED-SP: ADVISORY
```

## 6. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/ditto/
├── DittoBridgeConfig.java
├── DittoBridgeConfigLoader.java
├── DittoFeatureBinding.java
├── DittoSignalMapper.java
├── DittoClientService.java
├── DittoFeatureInput.java
├── DittoEventInput.java
└── DittoAdvisoryOutputAggregator.java
```

## 7. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Read-only against a local Ditto sandbox (`docker compose` from the Ditto repo). Twin event subscription + cache. |
| 2 | Write to advisory features only. Validator enforces `recommended_*` / `advisory_*` prefix. |
| 3 | **Not pursued.** Direct twin-command write to actual control features is out of scope for this bridge. |

## 8. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | Local Ditto sandbox | `docker compose up` from the Ditto eclipse-ditto/ditto-examples repo, register one thing | Bridge connects, emits `MeasurementSignal` per the configured feature within 2 s |
| **S8** | Twin update | Update a feature property via Ditto REST | Latest cache reflects within 1 publishing interval; signal emitted |
| **S9** | Non-advisory write rejected | Config has a write binding for feature `setpoint` (not `recommended_setpoint`) | `DittoBridgeConfigLoader.load()` throws |
| **S10** | Thing deleted | Delete one configured thing | Bridge emits `AlarmSignal(TWIN_OFFLINE)`; subsequent reads of that thing's features carry `Quality.UNCERTAIN` |
| **S11** | Reconnect | Kill the Ditto WebSocket | Bridge reconnects with backoff; advisory event emitted |

## 9. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Ditto-side schema drift | Per-binding feature path validated at config load. |
| R2 | Confusion between OPC UA bridge and Ditto bridge in a mixed deployment | Document: OPC UA is for the *signal-level* control loop; Ditto is for the *device-fleet* reasoning. They can coexist. |
| R3 | Policy-based access denial mid-run | Surface as `Quality.BAD` on affected bindings + advisory event; do not infer values. |

## 10. References

* Eclipse Ditto — `https://www.eclipse.org/ditto/`.
* Ditto Java client — `https://github.com/eclipse-ditto/ditto-clients`.
