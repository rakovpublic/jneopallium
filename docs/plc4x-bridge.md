# PLC4X bridge

> Companion to `01-PLC4X.md` (the spec) and `00-FRAMEWORK.md` (the universal
> contract). This file documents the PLC4X bridge's domain context, YAML
> schema, manual demo procedure, and regulatory posture — i.e. the §10
> Definition-of-Done items that don't belong in the spec itself.

## Domain context

Most factories run a long tail of legacy fieldbus controllers — Siemens S7,
Modbus TCP, EtherNet/IP, Beckhoff ADS, Allen-Bradley, Profinet — that pre-date
modelled OPC UA servers. [Apache PLC4X](https://plc4x.apache.org/) wraps each
of these behind one Java `PlcConnection` API: open by connection string, then
read or write protocol-native field expressions.

The PLC4X bridge is the natural sibling of the OPC UA bridge: same
`MeasurementSignal` / `AlarmSignal` / `ActuatorCommandSignal` types, same
`AbstractBridgeOutputAggregator`-based safety algorithm (interlocks → override
→ clamp → rate-limit → diff-suppress → audit), same JSONL audit schema, same
phased rollout (`SHADOW` → `ADVISORY` → per-loop `AUTONOMOUS`). The only
material difference is **transport**: PLC4X drivers are mostly poll-based, so
this bridge runs one polling thread per `PlcConnection` instead of an OPC UA
subscription.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/plc4x/
├── Plc4xConfig.java                  ← YAML record (immutable)
├── Plc4xConfigLoader.java            ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── Plc4xConnectionBinding.java       ← per-connection runtime view
├── Plc4xFieldBinding.java            ← write binding (BridgeBinding)
├── Plc4xSignalMapper.java            ← PlcResponseCode → Quality + value mapper
├── Plc4xDriver.java                  ← abstraction over PLC4X (open/read/write/close)
├── Plc4xResponseCode.java            ← bridge-local mirror of PlcResponseCode
├── Plc4xClientService.java           ← connection pool + per-binding scheduler
├── Plc4xMeasurementInput.java        ← IInitInput → MeasurementSignal
├── Plc4xEventInput.java              ← IInitInput → AlarmSignal (bit-decoded)
├── Plc4xCommandOutputAggregator.java ← extends AbstractBridgeOutputAggregator
├── Plc4xAuditOutput.java             ← local JSONL audit
├── Plc4xException.java
└── package-info.java
```

The bridge core compiles **without** the real PLC4X jars on the classpath —
all interactions go through `Plc4xDriver`. A production wiring binds it to a
thin adapter over `org.apache.plc4x.java.api.PlcDriverManager` +
`PlcConnection` + `PlcReadRequest` / `PlcWriteRequest`. Tests inject the
in-memory `StubPlc4xDriver` instead, so the test suite runs offline with no
PLCs, no docker images, and no flaky network.

## YAML schema reference

```yaml
connections:
  - id: "S7-LINE-A"
    connectionString: "s7://10.10.0.1?remote-rack=0&remote-slot=1"
    requestTimeout:  "PT5S"          # ISO-8601 duration; default PT5S
    keepAliveInterval: "PT10S"       # optional
  - id: "MODBUS-PUMPHOUSE"
    connectionString: "modbus-tcp://10.10.0.2:502?unit-identifier=1"
    requestTimeout:  "PT2S"

reads:
  - bindingId: "TIC-101"             # stable id, also the loop key for safety mode
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB1.DBD0:REAL"   # verbatim PLC4X field expression
    signalTag: "PLANT.TIC101.PV"     # ISA-95 tag attached to the signal
    pollIntervalMs: 250

writes:
  - bindingId: "TIC-101"
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB1.DBD8:REAL"
    signalTag: "PLANT.TIC101.SP"
    failSafeValue: 0.0               # written on interlock trip
    rampRateMaxPerSec: 2.0           # null = no rate limit
    minClampValue: 0.0               # null = no lower clamp
    maxClampValue: 100.0             # null = no upper clamp

events:
  - bindingId: "TROUBLE-ALARMS"
    connectionId: "S7-LINE-A"
    fieldAddress: "%DB100.DBW0:WORD" # alarm word, decoded by severity map
    signalTag: "PLANT.LINE_A.TROUBLE"
    pollIntervalMs: 1000
    severityMap:                      # bit mask → AlarmPriority
      "0x0001": "LOW"
      "0x0002": "HIGH"
      "0x0010": "CRITICAL"

audit:
  localAuditFile: "/var/log/jneopallium/plc4x-audit.jsonl"
  writeRejectedToAudit: true

perTagSafetyMode:
  TIC-101: SHADOW                    # SHADOW / ADVISORY / AUTONOMOUS

tickInterval: "PT0.25S"              # default
```

`fieldAddress` is the verbatim PLC4X field expression for the connected
driver. The bridge issues a one-shot `validate()` against every read/write/
event field at startup; any rejection fails fast (S9). Connection strings
whose scheme has no driver (e.g. `ads://…` without `plc4j-driver-beckhoff`)
fail at the same point with a "no driver registered for scheme" message
(S10).

`pollIntervalMs` per binding controls cadence. The bridge enforces a
single-thread scheduler per connection so one slow binding doesn't delay
another on the same controller. A reasonable production floor is 250 ms;
small S7 CPUs degrade above ~5 Hz total poll rate.

## Acceptance scenarios

Universal (00-FRAMEWORK §5):

* **S1** Pure read → `Plc4xMeasurementInput` emits a typed
  `MeasurementSignal` within 2 s.
* **S2** Bad quality propagates → driver disconnect flips
  `Quality.GOOD` to `Quality.BAD`/`UNCERTAIN`.
* **S3** SHADOW mode rejects writes → `audit.verdict=REJECTED reason=SHADOW_MODE`.
* **S4** Reconnect after disconnect → cache flips to error code, advisory
  alarm emitted, no buffered writes replayed.
* **S5** Audit file unwritable → `Plc4xAuditOutput.isDegraded() == true`,
  bridge keeps writing.
* **S6** Unknown tag → `audit.verdict=REJECTED reason=UNKNOWN_TAG`.

Bridge-specific (01-PLC4X.md §8):

* **S7** Multi-driver coexistence (S7 + Modbus in one config).
* **S8** Polling rate respected within ±20 %.
* **S9** Address rejected at startup → `connect()` throws
  `Plc4xException` with the bindingId.
* **S10** Driver missing for connection scheme → `connect()` throws.
* **S11** Severity-map decode produces the right `AlarmPriority`.

All of these are implemented in
`worker/src/test/java/com/rakovpublic/jneuropallium/worker/bridge/plc4x/Plc4xBridgeIntegrationTest.java`
and run as part of `mvn -pl worker test`.

## Manual demo procedure

The bridge is offline-testable via `StubPlc4xDriver`, but for an end-to-end
sanity check against a real PLC simulator you can wire it to
[OpenPLC](https://www.openplcproject.com/) running in docker:

1. `docker run -p 502:502 -p 8080:8080 openplc/openplc_v3` — exposes a
   Modbus TCP slave on port 502.
2. Build the project: `mvn -B clean verify`.
3. Drop a config at `/tmp/plc4x-bridge-demo.yaml`:
   ```yaml
   connections:
     - id: "OPENPLC"
       connectionString: "modbus-tcp://127.0.0.1:502?unit-identifier=1"
       requestTimeout: "PT2S"
   reads:
     - bindingId: "DEMO-COIL"
       connectionId: "OPENPLC"
       fieldAddress: "coil:0"
       signalTag: "DEMO.COIL.0"
       pollIntervalMs: 500
   writes: []
   events: []
   audit:
     localAuditFile: "/tmp/jneopallium-plc4x-audit.jsonl"
     writeRejectedToAudit: true
   perTagSafetyMode:
     DEMO-COIL: SHADOW
   tickInterval: "PT0.5S"
   ```
4. Wire a production `Plc4xDriver` adapter that delegates to PLC4X (see the
   "Production wiring" section below) and run a small bootstrap that:
   `Plc4xConfigLoader.load(path)` → `new Plc4xClientService(driver, cfg)` →
   `connect()` → loop reading `Plc4xMeasurementInput.readSignals()`.
5. Toggle the OpenPLC coil from its web UI on `:8080`. The audit file should
   show no entries (read-only demo); the printed signals should flip 0↔1
   within the poll interval.

## Production wiring

The bridge core does not depend on the PLC4X jars. Production binaries add
the dependency themselves and supply a `Plc4xDriver` adapter. Suggested
Maven coordinates (verify the latest release before merging):

```xml
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
<!-- Pull only the drivers you need -->
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
```

A minimal real-driver adapter:

```java
public final class Plc4xRealDriver implements Plc4xDriver {
    private final PlcDriverManager mgr = PlcDriverManager.getDefault();
    private final ConcurrentMap<String, PlcConnection> open = new ConcurrentHashMap<>();

    @Override public void open(String id, String connectionString) {
        try { open.put(id, mgr.getConnectionManager().getConnection(connectionString)); }
        catch (PlcConnectionException e) { throw new Plc4xException(e.getMessage(), e); }
    }
    // …read / write / validate / close: build PlcReadRequest/PlcWriteRequest
    // and translate PlcResponseCode → Plc4xResponseCode.
}
```

## Regulatory posture

| Aspect | Posture |
|--------|---------|
| Safety ceiling | `AUTONOMOUS` (per-loop), per 01-PLC4X.md |
| Default per-loop mode | `SHADOW` — every loop must be explicitly promoted |
| Interlock authority | Direct: a tripped `InterlockSignal` overwrites any pending command (00-FRAMEWORK §0.2) |
| Operator override | Holds the bound tag for the override TTL; defeats neuron-derived commands but **not** interlocks |
| Audit | One JSONL line per command; schema documented in 00-FRAMEWORK §4 |
| MoC | Hot-reload not supported; YAML changes follow your site's Management of Change procedure |
| PUT/GET on Siemens S7 | Production controllers often disable PUT/GET — confirm with the controls engineer before shipping a write binding |

Bridges that ever write to live process equipment must complete a Phase-2
shadow run with a documented observation period before any loop is promoted
to `AUTONOMOUS`. The observation criteria mirror the OPC UA bridge — see
`docs/opcua-bridge-architecture.md`.
