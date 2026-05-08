# Bridge 01 — Apache PLC4X (legacy PLC integration)

> **Prerequisite:** Read `00-FRAMEWORK.md` first. This spec assumes the universal contract — §0 ground rules, package convention, IInitInput/IOutputAggregator pattern, audit schema, S1–S6 acceptance scenarios — and only documents what is unique to PLC4X.

**Priority:** very high. **Safety ceiling:** `AUTONOMOUS` (per-loop).

## 1. Domain context

OPC UA is the modern, modelled, secure path into industrial systems — but most factories still expose only legacy fieldbus protocols on most of their PLCs. Siemens S7, Modbus TCP, EtherNet/IP, Beckhoff ADS, Allen-Bradley DF1, Profinet — each has its own client library, its own addressing, its own quality semantics.

[Apache PLC4X](https://plc4x.apache.org/) unifies these behind a single Java API: a `PlcConnection` is opened from a connection string (`s7://10.10.0.1`, `modbus-tcp://10.10.0.2:502`, `ads://10.10.0.3.1.1:851`, etc.), and read/write requests are constructed against protocol-native field expressions. PLC4X is Apache-licensed, actively maintained, and ships official drivers for the protocols above.

The PLC4X bridge is therefore the natural sibling of the OPC UA bridge: same signal types, same safety posture, same audit format, but a much wider addressable installed base.

## 2. Maven dependency

```xml
<!-- in parent <dependencyManagement> -->
<dependency>
    <groupId>org.apache.plc4x</groupId>
    <artifactId>plc4j-api</artifactId>
    <version>0.12.0</version>
</dependency>
<dependency>
    <groupId>org.apache.plc4x</groupId>
    <artifactId>plc4j-connection-pool</artifactId>
    <version>0.12.0</version>
</dependency>
<!-- Pull only the drivers you need; PLC4X drivers are independently versioned modules -->
<dependency>
    <groupId>org.apache.plc4x</groupId>
    <artifactId>plc4j-driver-s7</artifactId>
    <version>0.12.0</version>
</dependency>
<dependency>
    <groupId>org.apache.plc4x</groupId>
    <artifactId>plc4j-driver-modbus</artifactId>
    <version>0.12.0</version>
</dependency>
<dependency>
    <groupId>org.apache.plc4x</groupId>
    <artifactId>plc4j-driver-ethernet-ip</artifactId>
    <version>0.12.0</version>
</dependency>
```

Verify the latest released version before merging — PLC4X moves on its own cadence.

## 3. Architecture

```
┌──────────────────────┐    PlcReadRequest         ┌────────────────────────┐
│ Legacy PLC fleet:    │ ────────────────────────▶ │ Plc4xClientService     │
│  • Siemens S7-1500   │                           │  • PlcConnection pool  │
│  • Modbus TCP        │ ◀──── PlcWriteRequest ─── │  • polling scheduler   │
│  • EtherNet/IP       │                           │  • latest cache        │
│  • Beckhoff ADS      │                           │  • per-tag quality     │
└──────────────────────┘                           └─────┬───────────┬──────┘
                                                         │           │
                                            ┌────────────▼─┐  ┌──────▼──────┐
                                            │ Plc4xMeasure │  │ Plc4xEvent  │
                                            │ mentInput    │  │ Input       │
                                            │  → Measure-  │  │  → Alarm-   │
                                            │    mentSignal│  │    Signal   │
                                            └──────────────┘  └─────────────┘
                                                    ▼
                              [Standard Jneopallium pipeline — see framework §2]
                                                    │
                                                    ▼
                                    Plc4xCommandOutputAggregator
                                    (extends AbstractBridgeOutputAggregator)
```

Unlike OPC UA, PLC4X has no native subscription mechanism on most drivers — reads are **polled** at a configurable rate per binding. The `Plc4xClientService` runs a single scheduled thread per `PlcConnection` and updates the latest cache.

## 4. Signal mapping — reuse the industrial signals

| PLC4X read | Jneopallium signal | Notes |
|---|---|---|
| Numeric tag value (BOOL, INT, REAL, …) | `MeasurementSignal` (existing in `industrial/`) | `Quality` from PLC4X `PlcResponseCode` (`OK`→GOOD, `INVALID_*`→BAD, `ACCESS_DENIED`/`NOT_FOUND`→UNCERTAIN). |
| Alarm/diagnostic tag | `AlarmSignal` (existing) | Map vendor-specific severity codes per a per-bridge `AlarmSeverityMap` config block. |
| Discrete state tag (e.g. interlock pin) | `InterlockSignal` (existing) | Treat as tripped when the configured `trippedValue` matches. |

Writes:

| Jneopallium signal | PLC4X write | Notes |
|---|---|---|
| `SetpointSignal` | `PlcWriteRequest` typed by binding | Driver must support write; many S7 setups expose only read in production. |
| `ActuatorCommandSignal` | `PlcWriteRequest` | Subject to clamp / rate-limit / fail-safe per framework §2.2. |

**No new signal types are introduced.** PLC4X is a transport substitution for OPC UA, not a new signal domain.

## 5. Configuration

```yaml
connections:
  - id: "S7-LINE-A"
    connectionString: "s7://10.10.0.1?remote-rack=0&remote-slot=1"
    requestTimeout:  "PT5S"
    keepAliveInterval: "PT10S"
  - id: "MODBUS-PUMPHOUSE"
    connectionString: "modbus-tcp://10.10.0.2:502?unit-identifier=1"
    requestTimeout:  "PT2S"

reads:
  - bindingId: "TIC-101"
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB1.DBD0:REAL"        # PLC4X field expression
    signalTag: "PLANT.TIC101.PV"
    pollIntervalMs: 250
  - bindingId: "PUMP-RUN"
    connectionId: "MODBUS-PUMPHOUSE"
    fieldAddress: "coil:0"
    signalTag: "PLANT.PUMP01.STATE"
    pollIntervalMs: 500

writes:
  - bindingId: "TIC-101"
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB1.DBD8:REAL"
    signalTag: "PLANT.TIC101.SP"
    failSafeValue: 0.0
    rampRateMaxPerSec: 2.0
    minClampValue: 0.0
    maxClampValue: 100.0

events:
  - bindingId: "TROUBLE-ALARMS"
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB100.DBW0:WORD"      # alarm word, bit-decoded by mapper
    signalTag: "PLANT.LINE_A.TROUBLE"
    pollIntervalMs: 1000
    severityMap:
      "0x0001": "LOW"
      "0x0002": "HIGH"
      "0x0010": "CRITICAL"

audit:
  localAuditFile: "/var/log/jneopallium/plc4x-audit.jsonl"
  writeRejectedToAudit: true

perTagSafetyMode:
  TIC-101: SHADOW

tickInterval: "PT0.25S"
```

`fieldAddress` is the verbatim PLC4X field expression for the connected driver. Validate by issuing a one-shot `PlcReadRequest` at config load and failing fast if any address is rejected.

## 6. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/plc4x/
├── Plc4xConfig.java
├── Plc4xConfigLoader.java
├── Plc4xConnectionBinding.java
├── Plc4xFieldBinding.java
├── Plc4xSignalMapper.java
├── Plc4xClientService.java               (manages a Map<connectionId, PlcConnection>)
├── Plc4xMeasurementInput.java
├── Plc4xEventInput.java
└── Plc4xCommandOutputAggregator.java     (extends AbstractBridgeOutputAggregator)
```

## 7. Phase plan

| Phase | Goal | Branch |
|-------|------|--------|
| 1 | Read-only with polling. Drivers wired: S7, Modbus TCP at minimum. Quality propagation verified per S2. | `feat/plc4x-readonly` |
| 2 | Advisory writes. All loops `SHADOW`. EtherNet/IP, ADS drivers added. | `feat/plc4x-advisory` |
| 3 | Per-loop `AUTONOMOUS` after observation, identical promotion procedure to OPC UA. | `feat/plc4x-autonomous` |

## 8. Bridge-specific acceptance scenarios (in addition to S1–S6)

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | Multi-driver coexistence | One config with both S7 and Modbus connections; one read binding each | Both inputs produce `MeasurementSignal`s under their respective `signalTag`s within 2 s |
| **S8** | Polling rate respected | `pollIntervalMs: 1000` on a binding, simulator changes value every 100 ms | Cache update rate ≈ 1 Hz (±20 %); no missed-poll warnings |
| **S9** | Address rejected at startup | Bad `fieldAddress` in YAML | `Plc4xClientService.connect()` throws with a clear message; bridge does not start |
| **S10** | Driver missing | Config references `ads://…` but no `plc4j-driver-beckhoff` on classpath | Clear "no driver registered for scheme" message at startup |
| **S11** | Severity map decode | Alarm word changes to `0x0010` in simulator | Emitted `AlarmSignal` priority is `CRITICAL` per `severityMap` |

## 9. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Polling load on a small PLC CPU | Min `pollIntervalMs` per connection enforced in config; default 250 ms. |
| R2 | S7 PUT/GET disabled in production controllers | Spec must include "verify PUT/GET enabled with controls engineer" in the operator runbook. Many sites disable it for security and have to re-enable it. |
| R3 | Mixed-endian PLCs in a single config | Document per-driver byte-order conventions and surface a `byteOrder` config knob if PLC4X driver doesn't auto-handle. |
| R4 | PLC4X 0.x version drift between minor releases | Pin to a specific version in BOM; bump deliberately, never auto-update. |

## 10. References

* PLC4X driver feature matrix — `https://plc4x.apache.org/users/protocols/index.html`. Each driver has a different read/write/subscribe support level — verify before designing a binding.
* OPC UA bridge spec — for the safety algorithm. PLC4X reuses it verbatim via `AbstractBridgeOutputAggregator`.
