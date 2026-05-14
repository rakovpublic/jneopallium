# Bridge 15 — Nengo (hybrid SNN runtime integration)

> **Prerequisite:** Read `00-FRAMEWORK.md` first. This spec assumes the universal contract — §0 ground rules, the `IInitInput` / `IOutputAggregator` pattern, the audit schema, the S1–S6 acceptance scenarios — and only documents what is unique to Nengo.

**Priority:** high — strategic. This is the first bridge whose counterpart is *another cognitive runtime*, not a protocol or a device. It directly addresses Terry Stewart's observation that Nengo and Jneopallium are interesting *together*: Nengo as an SNN encoding/realization module, Jneopallium as the typed-signal master runtime that owns planning, safety, and audit.

**Safety ceiling:** `ADVISORY` by default. `AUTONOMOUS` is permitted only when the downstream Nengo output is wired to a *simulator* (`simulatorOnly: true`), exactly analogous to MAVLink and ROS 2.

**Package exception:** unlike protocol bridges, this one lives at `worker.integration.nengo` rather than `worker.bridge.nengo`. Nengo is a *peer runtime*, not an external protocol, and the integration is symmetric (input bridge + output bridge against a single counterpart). The "bridge" mental model still applies — same six rules, same audit schema, same acceptance scenarios — but the package name records the conceptual distinction.

---

## 1. Domain context

[Nengo](https://www.nengo.ai/) is a Python framework for building large-scale neural simulations using the Neural Engineering Framework (NEF) and Semantic Pointer Architecture (SPA). It excels at representing continuous vectors as spiking populations, decoding them with learned weights, and running on multiple backends (Nengo Core CPU, NengoDL/TensorFlow, NengoLoihi for Intel's neuromorphic hardware, NengoFPGA, NengoOCL).

Jneopallium is a typed-signal, safety-gated, audited runtime — strong on the discrete event side (interlocks, harm vetoes, planning, transparency) and intentionally weak on continuous vector encoding. Nengo is the opposite: superb at continuous vector encoding via spiking populations, intentionally agnostic about typed safety semantics.

The hybrid architecture this bridge implements:

```
[ Nengo input model ] → [ Nengo→Jneo input bridge ] → [ Jneopallium master runtime ] → [ Jneo→Nengo output bridge ] → [ Nengo output model ]
       (SNN encoding)       (NengoInputFrame /                (typed signals,                   (NengoOutputFrame /        (smooth vector
                             NengoDecodedStateSignal)          safety gates, audit)              ApprovedActionSignal)      realization)
```

Jneopallium is the master clock and the only safety authority. Nengo is an encoder on one side and a realizer on the other. There is **no all-Nengo control path that bypasses Jneopallium**.

The comparison baselines this enables (matching Terry's suggestion):

| Configuration | Encoding | Planning/Safety | Realization |
|---|---|---|---|
| **all-Jneopallium** | typed numeric signals | Jneopallium | typed motor command |
| **all-Nengo** | Nengo ensembles | Nengo SPA + learned cleanup | Nengo motor decoding |
| **hybrid (this bridge)** | Nengo ensembles | Jneopallium | Nengo motor decoding |

Result tables should report all three to make the comparison fair.

## 2. Dependencies

### 2.1 Java side (none new beyond Jackson, already in worker)

```xml
<!-- Already present from the framework -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

No new Java dependency. The bridge talks JSON over either a local file (for offline / CI replay) or a Unix domain socket (for live demos); both are JDK-native since Java 16.

### 2.2 Python side

```text
# requirements.txt for the Nengo counterpart
nengo>=4.0.0
numpy>=1.26.0
```

Optional, only if the demo wants GPU-accelerated encoding:
```text
nengo-dl>=3.6.0
tensorflow>=2.16.0
```

## 3. Transport — newline-delimited JSON

Two channels, both newline-delimited UTF-8 JSON ("JSONL"):

* **Channel A — Nengo → Jneopallium** (input frames).
  Default transport: Unix domain socket `/tmp/jneo-nengo-input.sock`. Fallback: append-only file `/tmp/jneo-nengo-input.jsonl` for deterministic CI/replay.
* **Channel B — Jneopallium → Nengo** (output frames).
  Default transport: Unix domain socket `/tmp/jneo-nengo-output.sock`. Fallback: append-only file.

One JSON object per line, no trailing comma, no embedded newlines. The bridge does not use gRPC, Protobuf, or zeromq in v1. The user-visible debug story is `tail -f /tmp/jneo-nengo-*.jsonl`. v2 (a lockstep gRPC bridge) is a separate spec; this bridge produces a clean interface boundary that swap is trivial.

Why JSON over a socket and not a queue/broker:
* Zero infrastructure — both ends start with one `socket()` call.
* Inspectable with `nc -U /tmp/jneo-nengo-input.sock` from any shell.
* Deterministic by construction in file mode — perfect for tests.
* Easy to bridge to a lockstep gRPC v2 later: the same frame schema serializes to Protobuf one-to-one.

## 4. Frame schema

Both directions share the envelope. Required fields are non-optional.

```json
{
  "schema_version":       "1",
  "source":               "NENGO_INPUT" | "JNEOPALLIUM_OUTPUT",
  "frame_id":             "f-000123",
  "sequence_no":          123,
  "timestamp_ms":         1762886400000,
  "valid_until_ms":       1762886400250,
  "safety_status":        "OK" | "DEGRADED" | "STOP",
  "values":               { "<label>": <number>, ... },
  "transparency_log_id":  "tx-…" | null
}
```

Field semantics:

* `frame_id` — opaque string, stable within a run, monotonically increasing.
* `sequence_no` — integer ≥0; gaps detected by receiver, audited.
* `timestamp_ms` — wall-clock at frame production (per framework §0.6).
* `valid_until_ms` — wall-clock after which the frame is **stale**. Stale frames MUST NOT influence output. The output bridge decays to STOP when the most recent frame is stale.
* `safety_status` — `OK` = use values; `DEGRADED` = use values but flag downstream; `STOP` = ignore values, drive zero or fail-safe.
* `values` — labeled vector. Labels are stable identifiers agreed in YAML, not array positions.
* `transparency_log_id` — optional pointer back into the Jneopallium audit log (`TransparencyLogSignal.txId`) so a Nengo-side observer can correlate.

A frame with any required field missing or any non-finite number is **rejected**, audited with `verdict=FAILED reason=FRAME_INVALID`, and never reaches a signal.

### 4.1 Example — Nengo → Jneopallium input frame

```json
{
  "schema_version": "1",
  "source": "NENGO_INPUT",
  "frame_id": "f-000042",
  "sequence_no": 42,
  "timestamp_ms": 1762886400000,
  "valid_until_ms": 1762886400250,
  "safety_status": "OK",
  "values": {
    "dx_target":     0.85,
    "dy_target":    -0.30,
    "obstacle_dx":   0.10,
    "obstacle_dy":   0.05,
    "human_risk":    0.02,
    "battery":       0.78
  },
  "transparency_log_id": null
}
```

### 4.2 Example — Jneopallium → Nengo output frame

```json
{
  "schema_version": "1",
  "source": "JNEOPALLIUM_OUTPUT",
  "frame_id": "f-000042",
  "sequence_no": 42,
  "timestamp_ms": 1762886400125,
  "valid_until_ms": 1762886400375,
  "safety_status": "OK",
  "values": {
    "vx": 0.42,
    "vy": -0.15
  },
  "transparency_log_id": "tx-2026-05-14T12:00:00.125Z-0042"
}
```

### 4.3 Example — emergency STOP

```json
{
  "schema_version": "1",
  "source": "JNEOPALLIUM_OUTPUT",
  "frame_id": "f-000051",
  "sequence_no": 51,
  "timestamp_ms": 1762886400750,
  "valid_until_ms": 1762886401000,
  "safety_status": "STOP",
  "values": { "vx": 0.0, "vy": 0.0 },
  "transparency_log_id": "tx-2026-05-14T12:00:00.750Z-0051-HARMVETO"
}
```

## 5. Architecture

```
                Python process (Nengo)              │              Java process (Jneopallium worker)
  ┌──────────────────────────────────────────┐     │      ┌──────────────────────────────────────────┐
  │ nengo_input_model.py                      │     │      │ NengoBridgeInputSource (IInitInput)       │
  │  • SPA / ensemble encoders                │     │      │  • opens Channel A                         │
  │  • emits decoded vector per tick          │     │      │  • parses NengoInputFrame                  │
  │  • writes NengoInputFrame to Channel A    │ ───▶│ ───▶ │  • emits NengoDecodedStateSignal           │
  └──────────────────────────────────────────┘     │      │                                             │
                                                   │      │ NengoInputMapper                            │
                                                   │      │  • routes labeled values to typed signals: │
                                                   │      │      dx/dy_target → SensorySignal(GOAL)    │
                                                   │      │      obstacle_*   → SensorySignal(PROXIM.) │
                                                   │      │      human_risk   → HarmAssessmentSignal   │
                                                   │      │      battery      → EfficiencySignal       │
                                                   │      └──────────────┬──────────────────────────────┘
                                                   │                     │
                                                   │      ┌──────────────▼──────────────────────────────┐
                                                   │      │ Standard Jneopallium pipeline               │
                                                   │      │  Layer 0..7 → PlanningNeuron → HarmGate     │
                                                   │      │  → SafetyMode → OperatorOverride            │
                                                   │      │  → audit                                     │
                                                   │      └──────────────┬──────────────────────────────┘
                                                   │                     │
                                                   │      ┌──────────────▼──────────────────────────────┐
                                                   │      │ JneopalliumToNengoMapper                     │
                                                   │      │  • turns approved MotorCommandSignal /       │
                                                   │      │    ApprovedActionSignal into NengoOutputFrame│
                                                   │      │                                              │
                                                   │      │ NengoBridgeOutputAggregator (IOutputAggreg.) │
                                                   │      │  • applies §0 rules (interlock/override/    │
                                                   │      │    SHADOW/clamp/rate-limit)                  │
                                                   │      │  • writes frame to Channel B                 │
                                                   │      └──────────────┬──────────────────────────────┘
                                                   │                     │
  ┌──────────────────────────────────────────┐     │                     ▼
  │ nengo_output_model.py                     │ ◀───────────── Channel B (JSONL)
  │  • reads NengoOutputFrame                 │     │
  │  • drives a Nengo ensemble that smooths   │     │
  │    [vx, vy] over a short tau              │     │
  │  • on stale or STOP, ramp to (0,0)        │     │
  │  • exposes the smoothed vector to the     │     │
  │    actual sim/robot adapter               │     │
  └──────────────────────────────────────────┘     │
```

## 6. Signal mapping

### 6.1 New Jneopallium signal classes (proposed)

Three new classes, all in `worker.integration.nengo`:

| Class | Role |
|---|---|
| `NengoDecodedStateSignal` | inbound carrier — labeled vector + frame metadata. Implements `IInputSignal<Void>`. Holds raw `Map<String,Double>` plus `safetyStatus`, `validUntilMs`, `frameId`. |
| `NengoVectorSignal` | outbound carrier — labeled output vector. Used internally between `JneopalliumToNengoMapper` and the aggregator. |
| `ApprovedActionSignal` (optional, proposed in `ai/signals/fast/`) | generic envelope for "this command passed the safety pipeline". `MotorCommandSignal` may carry this for the demo; the envelope is the right long-term shape if more action types appear. |

`NengoDecodedStateSignal` is intentionally **opaque** at this layer — the safety pipeline does **not** reason over it directly. `NengoInputMapper` converts it into typed signals before any neuron sees the data.

### 6.2 Input mapping (per-binding, configured in YAML)

| Frame label | Mapped to | Notes |
|---|---|---|
| `dx_target`, `dy_target` | `SensorySignal(modality=GOAL_RELATIVE)` | One signal per axis, or one 2-D signal — config choice. |
| `obstacle_dx`, `obstacle_dy` | `SensorySignal(modality=PROXIMITY)` | Quality `BAD` if frame's `safety_status` ≠ `OK`. |
| `human_risk` | `HarmAssessmentSignal` (in `ai/signals/fast/`) | Drives the existing harm gate. |
| `battery` | `EfficiencySignal` (or new `EnergyStateSignal`, see ROS 2 spec §6) | |
| any other label | `MeasurementSignal(tag=label)` | Default fallback so unknown labels don't crash. |

The mapping is in YAML, not in code. Adding a new sensor channel requires no recompile.

### 6.3 Output mapping

| Approved signal | Becomes frame `values` |
|---|---|
| `MotorCommandSignal` (2-D) | `{ "vx": …, "vy": … }` |
| `ApprovedActionSignal` (carrier) | inherits inner signal's labels |
| `HarmVetoSignal` | frame with `safety_status: "STOP"` and zero `values` |

## 7. Configuration

```yaml
transport:
  channelInPath:  "/tmp/jneo-nengo-input.sock"   # or *.jsonl for file mode
  channelOutPath: "/tmp/jneo-nengo-output.sock"
  mode: "UDS"                                     # or "FILE"
  reconnectBackoffMs: 250
  reconnectMaxMs:     5000
  frameMaxBytes:      65536

simulatorOnly: true     # MUST be true unless the Nengo output is going to a
                        # certified actuator chain. AUTONOMOUS rejected when
                        # simulatorOnly: false.

inputMappings:
  - frameLabel: "dx_target"
    signal: "SENSORY"
    modality: "GOAL_RELATIVE"
    signalTag: "ROBOT.GOAL.DX"
  - frameLabel: "dy_target"
    signal: "SENSORY"
    modality: "GOAL_RELATIVE"
    signalTag: "ROBOT.GOAL.DY"
  - frameLabel: "obstacle_dx"
    signal: "SENSORY"
    modality: "PROXIMITY"
    signalTag: "ROBOT.OBS.DX"
  - frameLabel: "obstacle_dy"
    signal: "SENSORY"
    modality: "PROXIMITY"
    signalTag: "ROBOT.OBS.DY"
  - frameLabel: "human_risk"
    signal: "HARM_ASSESSMENT"
    signalTag: "ROBOT.HARM.RISK"
  - frameLabel: "battery"
    signal: "EFFICIENCY"
    signalTag: "ROBOT.BATTERY"

outputMappings:
  - approvedSignalType: "MotorCommandSignal"
    frameLabels: ["vx", "vy"]
    validForMs: 250
    failSafeFrame:
      safety_status: "STOP"
      values: { "vx": 0.0, "vy": 0.0 }

watchdog:
  staleFrameMs: 250            # if no fresh input for this long, drop quality
                               # to BAD on derived signals
  outputDecayMs: 250           # downstream Nengo decays to STOP within this

audit:
  localAuditFile: "/var/log/jneopallium/nengo-audit.jsonl"

perTagSafetyMode:
  ROBOT.MOTOR: ADVISORY        # AUTONOMOUS allowed only if simulatorOnly=true
```

## 8. Java package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/integration/nengo/
├── NengoBridgeConfig.java                  (record, immutable)
├── NengoBridgeConfigLoader.java            (Jackson YAML, FAIL_ON_UNKNOWN_PROPERTIES)
├── NengoInputFrame.java                    (record, Jackson-mapped)
├── NengoOutputFrame.java                   (record, Jackson-mapped)
├── NengoDecodedStateSignal.java            (extends AbstractSignal, IInputSignal)
├── NengoVectorSignal.java                  (internal carrier)
├── NengoChannelService.java                (UDS or file transport, AutoCloseable)
├── NengoBridgeInputSource.java             (IInitInput — reads Channel A)
├── NengoInputMapper.java                   (NengoDecodedStateSignal → typed signals)
├── JneopalliumToNengoMapper.java           (approved signal → NengoOutputFrame)
├── NengoBridgeOutputAggregator.java        (IOutputAggregator — writes Channel B,
│                                            extends AbstractBridgeOutputAggregator
│                                            from worker.bridge.common per framework §6)
└── package-info.java
```

Optional, if added: `worker/.../net/signals/impl/ai/fast/ApprovedActionSignal.java`.

## 9. Python files (delivered alongside the bridge)

Location in repo: `worker/src/test/python/nengo/` (the worker's `pom.xml` should NOT compile or import this directory — it's runnable artifacts only).

```
worker/src/test/python/nengo/
├── README.md                       (mirrored §13 below)
├── requirements.txt
├── nengo_frame.py                  (shared schema dataclass + JSONL I/O helpers)
├── nengo_input_model.py            (sensor encoder + frame writer)
├── nengo_output_model.py           (frame reader + smoothing ensemble + decoder)
└── run_hybrid_demo.py              (orchestrator: starts both Python models +
                                     prints expected matching Jneopallium command)
```

### 9.1 `nengo_frame.py` — minimum required helpers

```python
import json, time, dataclasses, os
from typing import Dict, Optional

@dataclasses.dataclass
class NengoFrame:
    schema_version: str
    source: str
    frame_id: str
    sequence_no: int
    timestamp_ms: int
    valid_until_ms: int
    safety_status: str
    values: Dict[str, float]
    transparency_log_id: Optional[str] = None

    def to_jsonl(self) -> bytes:
        return (json.dumps(dataclasses.asdict(self)) + "\n").encode("utf-8")

    @classmethod
    def from_line(cls, line: str) -> "NengoFrame":
        obj = json.loads(line)
        # accept either source; caller validates.
        return cls(**obj)

def now_ms() -> int:
    return int(time.time() * 1000)
```

### 9.2 `nengo_input_model.py` — outline

```python
import nengo, numpy as np, socket, time
from nengo_frame import NengoFrame, now_ms

# Sensor-vector encoder. In a real demo the encoder would observe an
# environment; here we drive it from a scripted trajectory.
model = nengo.Network(label="sensor-encoder")
with model:
    sensor_input = nengo.Node(lambda t: scripted_sensor(t), size_out=6)
    ens = nengo.Ensemble(n_neurons=600, dimensions=6)
    nengo.Connection(sensor_input, ens)
    decoded = nengo.Probe(ens, synapse=0.01)

with nengo.Simulator(model, dt=0.01, progress_bar=False) as sim, \
     open_channel_a() as ch:
    seq = 0
    while running():
        sim.step()
        vec = sim.data[decoded][-1]            # latest decoded vector
        frame = NengoFrame(
            schema_version="1",
            source="NENGO_INPUT",
            frame_id=f"f-{seq:06d}",
            sequence_no=seq,
            timestamp_ms=now_ms(),
            valid_until_ms=now_ms() + 250,
            safety_status="OK",
            values={
                "dx_target":   float(vec[0]),
                "dy_target":   float(vec[1]),
                "obstacle_dx": float(vec[2]),
                "obstacle_dy": float(vec[3]),
                "human_risk":  float(vec[4]),
                "battery":     float(vec[5]),
            },
        )
        ch.write(frame.to_jsonl())
        seq += 1
        time.sleep(0.01)
```

`open_channel_a()` is either a Unix-domain-socket writer or a JSONL file appender per the YAML `transport.mode`.

### 9.3 `nengo_output_model.py` — outline

```python
import nengo, numpy as np
from nengo_frame import NengoFrame, now_ms

# Smoothing decoder.
model = nengo.Network(label="motor-decoder")
with model:
    target = nengo.Node(np.zeros(2), size_in=0, size_out=2)
    motor = nengo.Ensemble(n_neurons=400, dimensions=2)
    nengo.Connection(target, motor, synapse=0.05)  # short smoothing tau
    out = nengo.Probe(motor, synapse=0.05)

with nengo.Simulator(model, dt=0.01, progress_bar=False) as sim, \
     open_channel_b() as ch:
    target_vec = np.zeros(2)
    last_valid_until = 0
    while running():
        for line in ch.poll_lines(max_lines=10):
            frame = NengoFrame.from_line(line)
            if frame.safety_status == "STOP":
                target_vec = np.zeros(2)
            elif frame.valid_until_ms < now_ms():
                continue                             # stale, ignore
            else:
                target_vec = np.array([frame.values["vx"],
                                       frame.values["vy"]])
                last_valid_until = frame.valid_until_ms

        # Watchdog: if no fresh frame, decay target to zero.
        if now_ms() > last_valid_until:
            target_vec *= 0.0

        target.output = target_vec
        sim.step()
        # sim.data[out][-1] is the smoothed motor vector that the
        # downstream sim/robot adapter consumes.
```

### 9.4 `run_hybrid_demo.py`

Orchestrates the two Python models plus prints, on each frame, the matching expected Jneopallium command (so a human can sanity-check without reading the audit log). Runs for a fixed duration (default 10 s) or until SIGINT.

## 10. Phase plan

| Phase | Goal |
|-------|------|
| 1 | File-mode JSONL transport. One-shot demo: write 10 frames from `nengo_input_model.py`, Java reads them and produces typed signals visible in worker logs. |
| 2 | Unix-domain-socket transport. Live two-Python-process + one-Java-process demo. Watchdog (`outputDecayMs`) verified. |
| 3 | Output bridge wired: approved `MotorCommandSignal` → output frame → smoothed motor vector consumed by `nengo_output_model.py`. |
| 4 | Demo: 2-D robot navigation in a tiny grid simulator. Three runs side-by-side: all-Jneopallium, all-Nengo, hybrid. Audit logs for the hybrid run are inspected to confirm every approved command was visible. |
| 5 (deferred) | gRPC lockstep bridge. Same frame schema, Protobuf-serialised. Per-tick synchronisation rather than free-running. **Not in v1.** |

## 11. Bridge-specific acceptance scenarios (in addition to S1–S6)

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | File-mode end-to-end | `transport.mode: FILE`. Write 10 frames manually, run worker | 10 `NengoDecodedStateSignal` instances appear; mapper produces typed signals; no exceptions |
| **S8** | UDS reconnect | Live Python writer killed and restarted | Java side reconnects within `reconnectBackoffMs`; subsequent frames flow; advisory `AlarmSignal(BRIDGE_RECONNECTED)` emitted |
| **S9** | Stale frame ignored | Frame with `valid_until_ms` already past | Frame produces no signal; one audit `verdict=REJECTED reason=FRAME_STALE` |
| **S10** | Invalid frame rejected | Frame missing `safety_status` field | One audit `verdict=FAILED reason=FRAME_INVALID`; pipeline continues |
| **S11** | Watchdog decay | Stop the Python input model. Output bridge stops receiving fresh approved commands | Within `outputDecayMs`, the bridge writes one STOP frame to Channel B with `safety_status: "STOP"`, `values: {vx:0, vy:0}` |
| **S12** | STOP propagation | Force a harm veto in the pipeline | Output frame has `safety_status: "STOP"`; `transparency_log_id` is the harm-veto's tx id |
| **S13** | Transparency correlation | Approved frame's `transparency_log_id` looked up in the worker's local audit JSONL | Match found with same tx id and the contributing neurons listed |
| **S14** | Cross-runtime determinism (file mode) | Replay a recorded `jneo-nengo-input.jsonl` twice through the bridge with identical seeds | Both runs produce byte-identical `jneo-nengo-output.jsonl` |
| **S15** | simulatorOnly enforcement | Config `perTagSafetyMode: {ROBOT.MOTOR: AUTONOMOUS}`, `simulatorOnly: false` | `NengoBridgeConfigLoader.load()` throws |

## 12. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Python and Java run at different wall-clock rates → drift | Watchdog (`staleFrameMs` + `outputDecayMs`) on both sides; explicit ranges in audit. gRPC lockstep (deferred phase 5) is the long-term fix. |
| R2 | Hybrid demo gives Nengo or Jneopallium an unfair advantage | The all-X baselines are mandatory; comparison results report all three configurations. |
| R3 | Schema drift between Java and Python frame classes | `schema_version` field; receiver rejects on mismatch; both languages share a single JSON-schema file that's the source of truth in CI. |
| R4 | Bytecode injection / arbitrary JSON load | Jackson configured `FAIL_ON_UNKNOWN_PROPERTIES = true`, max frame size enforced, only well-known field types accepted. |
| R5 | UDS not available on Windows pre-2023 builds | File mode is the always-available fallback; UDS is preferred for live demos. |
| R6 | Confusing this bridge with a *Nengo backend* | This is **not** a Nengo backend. Jneopallium does not run on Nengo. The bridge is a peer-to-peer adapter at the signal level. Documentation makes this explicit. |

## 13. README content (mirrored in `worker/src/test/python/nengo/README.md`)

* **What this is.** A two-direction JSONL bridge between a Nengo Python process and the Jneopallium Java worker. Jneopallium is the master runtime; Nengo is the input encoder and output realizer.
* **Architecture.** (Copy §5 diagram.)
* **How to run.**
    1. `pip install -r requirements.txt`.
    2. Choose transport mode. For first-time setup use file mode: edit `nengo-bridge.yml` to set `transport.mode: FILE`.
    3. Start the worker with the Nengo bridge enabled: `mvn -pl worker exec:java -Dexec.mainClass=...NengoBridgeDemoMain`.
    4. In one shell, `python nengo_input_model.py`. In another, `python nengo_output_model.py`.
    5. In a third shell, `tail -f /tmp/jneo-nengo-*.jsonl` to inspect frames live.
    6. For the orchestrated demo: `python run_hybrid_demo.py` (starts both Python models with a scripted scenario).
* **JSON frame examples.** Copy §4.1, §4.2, §4.3.
* **Limitations.**
    * No per-tick lockstep — both sides run their own clocks. Phase 5 (gRPC lockstep) addresses this.
    * Only the simple 2-D navigation demo signals are mapped in YAML; adding new signals requires a YAML edit on the Java side and a values-dict entry on the Python side.
    * Quality is binary (`OK` / `DEGRADED` / `STOP`), not the full Jneopallium `Quality` enum. The mapper degrades to `BAD` on `DEGRADED`/`STOP` frames.
    * No streaming backpressure across the channel. Frame queue is bounded; on overflow, oldest dropped with audit.
    * UDS path is local-machine only by design.
* **Next step.** gRPC lockstep bridge, separate spec. Same frame schema serialized to Protobuf. Per-tick `step()` synchronisation. Required for any deployment where Jneopallium and Nengo must agree on simulated time to the millisecond.

## 14. References

* Nengo — `https://www.nengo.ai/`.
* Neural Engineering Framework — Eliasmith & Anderson, *Neural Engineering* (MIT Press, 2003).
* Semantic Pointer Architecture — `https://www.nengo.ai/nengo-spa/`.
* Jneopallium OPC UA bridge — the prototype that established §0 (`JNEOPALLIUM_OPCUA_INTEGRATION.md` in the main repo).
* MAVLink bridge spec — same `simulatorOnly` discipline applied here.
