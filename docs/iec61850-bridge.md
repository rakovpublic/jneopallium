# IEC 61850 bridge

> Companion to [`11-IEC61850.md`](../11-IEC61850.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This
> file documents the IEC 61850 bridge's domain context, YAML schema,
> manual demo procedure, and regulatory posture — the §10
> Definition-of-Done items that don't belong in the spec itself.

## Domain context

[IEC 61850](https://webstore.iec.ch/publication/6028) is the
interoperability standard for electrical substations and power utility
automation. It defines a five-level data model
(`LDevice / LN / DO / DA / data-attribute-leaf`), three information
services (MMS client/server for reads, GOOSE multicast for fast event
exchange, Sampled Values for synchrophasor-rate streams), and the
Substation Configuration Language (SCL) — the XML engineering exchange
format that describes the entire substation configuration.

Substations expose **measurements** (currents, voltages, frequencies,
power factors), **status** (breaker open/closed, isolator position),
**events** (protection trips, alarms) and **control** (operate,
select-before-operate, cancel). For Jneopallium the bridge reads the
first three. **Control is permanently out of scope.**

## Why READ-ONLY

* IEC 61850 control writes use select-before-operate (SBO) with operator
  confirmation; wrapping that in a JVM-resident bridge undermines the
  protocol's safety model.
* Substation control is a SIL-classified function (SIL 2/3) that lives on
  certified RTUs and protection relays. A Java bridge writing to a
  Logical Node's `Oper` attribute would act outside its certification
  scope.
* Reads are sufficient to add value: state-estimation cross-checks,
  anomaly detection, equipment-degradation modelling, demand-response
  advisory.

A bridge that opens a write surface here is a *different* bridge — name
it `iec61850-control` and certify it separately. Do not extend this one.
This package contains no aggregator or output class (11-IEC61850.md §7),
and its `Iec61850MmsClient` seam exposes no method that writes a Data
Attribute. The `writes:` block in YAML is rejected at config-load.

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/iec61850/
├── Iec61850BridgeConfig.java          ← YAML record (writes: rejected at load)
├── Iec61850BridgeConfigLoader.java    ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── Iec61850DaBinding.java             ← BridgeBinding for one Data Attribute read
├── SclParser.java                     ← Stax SCL parser (.icd/.cid/.scd)
├── Iec61850SignalMapper.java          ← MMS read / report → MeasurementSignal/AlarmSignal
├── Iec61850MmsClient.java             ← read-only transport seam (no write methods)
├── InMemoryIec61850MmsClient.java     ← in-memory simulator used by tests
├── Iec61850ClientService.java         ← orchestrator: SCL load, polling, RCB subs
├── Iec61850MeasurementInput.java      ← IInitInput → MeasurementSignal
├── Iec61850EventInput.java            ← IInitInput → AlarmSignal (RCB reports)
├── Iec61850AuditOutput.java           ← local JSONL audit
└── package-info.java
```

No aggregator class. No output class. There is no write code path.

## YAML schema

The canonical example lives in 11-IEC61850.md §6. Key points:

* `ied[].sclFile` is required per IED. The bridge **fails fast** at
  startup if the file is missing or malformed — SCL is the ground-truth
  data model and unresolved DA paths are unsafe to silently approximate.
* `reads[]` is a flat list of Data Attribute reads. `targetSignal` is
  optional: paths matching `XCBR*` / `XSWI*` auto-classify as `STATUS`,
  everything else as `MEASUREMENT`.
* `events[]` subscribes to Report Control Blocks. `severityMap` keys are
  IEC 61850 Logical Node classes (`PIOC`, `PTOC`, `PTUV`, …) and values
  are taken from the `CRITICAL / HIGH / LOW / JOURNAL` vocabulary; the
  bridge maps `CRITICAL` → ISA-18.2 `URGENT` (the closest in-tree priority).
* GOOSE listener is **not wired in v1** (11-IEC61850.md §8 phase 3). The
  per-binding-id queues are already in place; adding GOOSE is a matter
  of slotting a new transport seam that calls
  `Iec61850ClientService.onReport` — the rest of the pipeline is
  protocol-agnostic.
* `audit.localAuditFile` is the JSONL audit destination conforming to
  00-FRAMEWORK §4. Audit failures are isolated per framework S5 — the
  bridge continues running in degraded mode.

## Manual demo

Phase 1 acceptance is exercised against the simulator that ships with
`libiec61850` (`server_example_basic_io`). Manual procedure:

1. Build and run the libiec61850 example server on `localhost:102` —
   it advertises an `LD0` Logical Device with `MMXU1`, `MMXU2`, `XCBR1`
   and an `LLN0` reporting facility.
2. Drop a minimal SCL describing those nodes into
   `/etc/jneopallium/scl/SUB1.icd`. (For phase-1 smoke testing the SCL
   shipped in `Iec61850BridgeIntegrationTest` is sufficient.)
3. Wire the bridge with `Iec61850BridgeConfigLoader.load(yamlPath)`,
   instantiate the production MMS client (a `iec61850bean`-backed
   implementation of `Iec61850MmsClient`) and tick `svc.poll()`.
4. Watch the per-binding queues drain through
   `Iec61850MeasurementInput.readSignals()` — each binding should produce
   one `MeasurementSignal` per cycle, carrying the source-system
   timestamp.
5. Force a protection trip on the simulator and confirm
   `Iec61850EventInput.readSignals()` returns an `AlarmSignal` of priority
   `URGENT` (per the `severityMap`).

Acceptance tests (`mvn -pl worker -Dtest='Iec61850*,Scl*' test`) cover
the no-simulator path with `InMemoryIec61850MmsClient`. Production
sites need a real MMS adapter, which slots in unchanged behind the
`Iec61850MmsClient` seam.

## Regulatory posture

The IEC 61850 bridge is **READ-ONLY initially** and remains so until a
separately-certified `iec61850-control` bridge is built. It contributes
only advisory observations to the Jneopallium pipeline:

* Protection trips become `AlarmSignal(URGENT, "PROTECTION_OPERATE")` —
  awareness, not commands.
* Breaker positions become `MeasurementSignal` with value `0.0` (OPEN)
  or `1.0` (CLOSED) — state observation, not directive. The framework's
  `InterlockSignal` is reserved for the aggregator's interlock-trip
  path (00-FRAMEWORK §0 rule 2) and is not produced by this bridge.
* Measurement quality from the IEC 61850 `q` attribute propagates
  unmodified into `Quality.GOOD/BAD/UNCERTAIN` — untrustworthy data is
  never silently promoted to "good" (framework rule 5).
* Source-system timestamps (the IEC 61850 `t` attribute) always win
  over `System.currentTimeMillis()` (framework rule 6).

Time sync for sampled-values use cases (PTP / IRIG-B) is **out of scope
for v1**; sampled-values support is deferred and use-cases requiring it
are documented as future work (11-IEC61850.md §10 R4).
