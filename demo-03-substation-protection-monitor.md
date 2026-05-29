# Demo 03 — Substation protection monitor (IEC 61850 / MMS, READ-ONLY)

> Bridge: **IEC 61850** ([doc](../iec61850-bridge.md)) ·
> Module: **[industrial](../modules/industrial.md)** (validation / alarm layers)
> · Safety ceiling: **READ-ONLY** (structural — no output path exists) ·
> External system: an SCL/CID file describing the bay; the bridge ships an
> `InMemoryIec61850MmsClient` so the demo runs with **no physical IED**.

A pure observation demo. It shows the framework consuming a substation's
MMS/GOOSE data model, propagating IEC 61850 quality bits into typed signals,
aggregating protection events into operator alarms — and being **structurally
incapable** of writing anything back. This is the strongest end of the safety
spectrum: read-only is enforced by the absence of an aggregator, not by a flag.

## Scenario

A distribution feeder bay has an IED publishing a dataset over MMS: busbar
voltage (`MMXU.PhV`), feeder current (`MMXU.A`), circuit-breaker position
(`XCBR.Pos`), and a protection trip indication (`PTOC.Op` — time-overcurrent
operate). The monitor reads these, flags bad-quality measurements, and raises a
prioritised operator alarm when the breaker opens or protection operates. It
provides situational awareness to the control room; it never issues a control
(`SBO`/`Oper`) write.

## What it demonstrates

| Feature | Where |
|---|---|
| SCL/CID model parsing into runtime data-attribute bindings | `SclParser`, `Iec61850DaBinding` |
| MMS reads → typed measurements with quality | `Iec61850MeasurementInput`, `Iec61850SignalMapper` |
| IEC 61850 quality bits → `Quality.GOOD/UNCERTAIN/BAD` (framework §0.5) | `Iec61850SignalMapper` |
| Protection/CB events → prioritised alarms | `Iec61850EventInput` → `AlarmAggregationNeuron` |
| Server-sourced timestamps (`t` attribute) over local clock | `Iec61850SignalMapper` |
| **Structural READ-ONLY** — there is no output aggregator to wire | (by construction) |
| In-memory MMS test seam for IED-free runs | `InMemoryIec61850MmsClient` |

## Architecture / data flow

```
 Bay IED (or InMemoryIec61850MmsClient driven by an SCL/CID file)
   LD0/MMXU1.PhV   LD0/MMXU1.A   LD0/XCBR1.Pos   LD0/PTOC1.Op
        │ MMS report / poll (value + quality + t)
        ▼
  ┌──────────────────────────────────────────────┐
  │ Iec61850ClientService (Iec61850MmsClient)     │
  │  • dataset reports   • quality + timestamp     │
  └───┬───────────────────────────┬───────────────┘
      ▼                           ▼
 Iec61850MeasurementInput     Iec61850EventInput
   → MeasurementSignal          → AlarmSignal (CB_OPEN / PROT_OP)
   (Quality from MMS q-bits)
      │                           │
      ▼                           ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Monitor sub-net:                                          │
  │  MeasurementValidator (range + rate; downgrades quality)  │
  │  AlarmAggregation (ISA-18.2 suppression, priority)        │
  │  ModeController (display state: NORMAL / ALARM)           │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
              Operator advisory dashboard  +  Iec61850AuditOutput (read log)
                              ▲
                              └──  NO write path. No SBO/Oper. By design.
```

## Components used

* **Signals**: `MeasurementSignal` (voltage, current — 1/1),
  `AlarmSignal` (CB open / protection operate — 1/1). `InterlockSignal` may be
  derived read-only for display but is never actuated.
* **Neurons**: `MeasurementValidatorNeuron`, `AlarmAggregationNeuron`,
  `ModeControllerNeuron` (used here only to drive a display state machine).
* **Processors**: `MeasurementValidationProcessor`, `AlarmAggregationProcessor`.
* **Bridge** (`worker.bridge.iec61850`): `Iec61850BridgeConfigLoader`,
  `SclParser`, `Iec61850DaBinding`, `Iec61850ClientService`,
  `Iec61850MmsClient` / `InMemoryIec61850MmsClient`, `Iec61850SignalMapper`,
  `Iec61850MeasurementInput`, `Iec61850EventInput`, `Iec61850AuditOutput`.

Note the conspicuous absence: there is **no** `Iec61850CommandOutputAggregator`.
Read-only is a structural property of the bridge, not a runtime mode.

## Configuration

`/tmp/demo03-substation.yaml`:

```yaml
connection:
  host: "127.0.0.1"          # ignored when using the in-memory client
  port: 102
  iedName: "BAYCTRL"
  sclFile: "/tmp/bay.cid"    # SCL/CID describing the logical nodes below
  useInMemoryClient: true    # demo: no physical IED required

reads:
  - bindingId: "BUS-V"
    reference: "BAYCTRL/LD0.MMXU1.PhV.phsA.cVal.mag.f"
    signalTag: "GRID.BUS.V"
  - bindingId: "FEEDER-I"
    reference: "BAYCTRL/LD0.MMXU1.A.phsA.cVal.mag.f"
    signalTag: "GRID.FEEDER.I"

events:
  - bindingId: "CB-POS"
    reference: "BAYCTRL/LD0.XCBR1.Pos.stVal"
    signalTag: "GRID.CB.POS"
    onValue: "OPEN"          # → AlarmSignal CB_OPEN
  - bindingId: "PROT-OP"
    reference: "BAYCTRL/LD0.PTOC1.Op.general"
    signalTag: "GRID.PTOC.OP"
    onValue: "true"          # → AlarmSignal PROT_OP (HIGH)

severityMap:
  PROT_OP: HIGH
  CB_OPEN: HIGH
audit:
  localAuditFile: "/tmp/jneopallium-demo03-readlog.jsonl"
tickInterval: "PT0.25S"
# (no `writes:` block — the schema does not define one for this bridge)
```

## Run procedure

1. **Provide an SCL/CID.** Use any valid `.cid` for a bay containing `MMXU`,
   `XCBR`, and `PTOC` logical nodes (vendor sample files or one exported from an
   IEC 61850 engineering tool). The `InMemoryIec61850MmsClient` reads the model
   from this file and lets you drive attribute values programmatically.

2. **Build and wire (read-only):**

   ```java
   var cfg   = Iec61850BridgeConfigLoader.load(Path.of("/tmp/demo03-substation.yaml"));
   var client = new InMemoryIec61850MmsClient(SclParser.parse(Path.of(cfg.connection().sclFile())));
   var svc   = new Iec61850ClientService(cfg, client);
   var mapper = new Iec61850SignalMapper(cfg);
   var audit  = new Iec61850AuditOutput(Path.of(cfg.audit().localAuditFile()));

   var measIn = new Iec61850MeasurementInput("iec-meas", svc, mapper, List.of("BUS-V","FEEDER-I"));
   var evtIn  = new Iec61850EventInput("iec-evt", svc, mapper);
   // build: validator → alarmAggregation → modeController (display only)
   // there is intentionally no output aggregator to construct.
   ```

3. **Stream nominal values.** Drive `MMXU1.PhV ≈ 11 kV`, `MMXU1.A` within rating,
   `XCBR1.Pos = CLOSED`. Confirm `MeasurementSignal`s arrive each tick with
   `Quality.GOOD` and the display state is `NORMAL`.

4. **Inject bad quality.** Set the MMS quality bit `validity=questionable` (or
   `invalid`) on `MMXU1.A`. The mapper must emit that measurement with
   `Quality.UNCERTAIN` (or `BAD`) — never `GOOD`. The validator may further
   downgrade on range/rate but never drops the sample.

5. **Operate protection.** Set `PTOC1.Op.general = true` and `XCBR1.Pos = OPEN`.
   `Iec61850EventInput` emits `AlarmSignal(HIGH, PROT_OP)` and
   `AlarmSignal(HIGH, CB_OPEN)`; `AlarmAggregationNeuron` prioritises and
   suppresses duplicates; the display state goes to `ALARM`.

6. **Confirm read-only by construction.** Attempt to obtain a write path — there
   is none in the API. The audit file logs reads/alarms only; no command record
   can exist.

## Acceptance

* Nominal stream yields `Quality.GOOD` measurements and a `NORMAL` display state.
* A questionable/invalid MMS quality bit propagates to `UNCERTAIN`/`BAD`; the
  sample is downgraded, never silently dropped.
* Protection operate + breaker open produce prioritised `HIGH` alarms with
  server-sourced timestamps; duplicates are suppressed.
* No control write is possible — the bridge exposes no output aggregator.

## Safety / regulatory posture

IEC 61850 control (`SBO`/`Oper`, GOOSE trip) is safety-critical and latency-
bounded in ways a cognitive supervisor must not arbitrate; this bridge is
therefore **READ-ONLY by construction**. It complements certified protection and
SCADA rather than competing with them: situational awareness, alarm rationalisation,
and trend capture for the control room. Promotion to any write capability would
require a new, separately-reviewed aggregator and is out of scope. See
[`../iec61850-bridge.md`](../iec61850-bridge.md).
