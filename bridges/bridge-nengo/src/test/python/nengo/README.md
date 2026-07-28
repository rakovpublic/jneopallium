# Nengo ↔ Jneopallium bridge — Python counterpart

This directory holds the **runnable Python artifacts** for the Nengo
integration described in [`../../../../../15-NENGO.md`](../../../../../15-NENGO.md).
The Java worker's `pom.xml` does **not** compile or import any code from
this tree — these are standalone scripts that talk JSONL to the worker
over a Unix-domain socket (default) or a file (fallback for CI replay).

Two channels, both newline-delimited UTF-8 JSON:

* **Channel A** — Nengo → Jneopallium input frames at
  `/tmp/jneo-nengo-input.sock` (or `/tmp/jneo-nengo-input.jsonl` in FILE mode).
* **Channel B** — Jneopallium → Nengo output frames at
  `/tmp/jneo-nengo-output.sock` (or `/tmp/jneo-nengo-output.jsonl`).

## What this is

A two-direction JSONL bridge between a Nengo Python process and the
Jneopallium Java worker. Jneopallium is the master runtime; Nengo acts
as the input encoder (spiking-population sensor decoding) and the
output realizer (smooth motor-vector decoding).

## Architecture

```
[ Nengo input model ] → [ Channel A — JSONL ] → [ Jneopallium worker ] → [ Channel B — JSONL ] → [ Nengo output model ]
   (SNN encoding)                                  (typed signals,                                  (smooth vector
                                                    safety, audit)                                   realization)
```

Jneopallium is the master clock and the only safety authority. Nengo is
an encoder on one side and a realizer on the other. There is **no
all-Nengo control path that bypasses Jneopallium**.

## How to run

1. `pip install -r requirements.txt`.
2. Choose transport mode. For first-time setup use file mode by setting
   `transport.mode: FILE` in your `nengo-bridge.yml`. For live demos use
   `UDS`.
3. Start the worker with the Nengo bridge enabled (see
   `15-NENGO.md` §10 Phase 1 for the minimal driver).
4. In one shell, run `python nengo_input_model.py`. In another,
   `python nengo_output_model.py`.
5. In a third shell, `tail -f /tmp/jneo-nengo-*.jsonl` to inspect frames
   live (file mode), or `nc -U /tmp/jneo-nengo-input.sock` to poke the
   UDS socket directly.
6. For the orchestrated demo: `python run_hybrid_demo.py`. This starts
   both Python models with a scripted 10-second scenario and prints the
   motor decoder's output every 50 steps so you can confirm the loop is
   closing.

## JSON frame examples

### Nengo → Jneopallium input frame

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
  }
}
```

### Jneopallium → Nengo output frame

```json
{
  "schema_version": "1",
  "source": "JNEOPALLIUM_OUTPUT",
  "frame_id": "f-000042",
  "sequence_no": 42,
  "timestamp_ms": 1762886400125,
  "valid_until_ms": 1762886400375,
  "safety_status": "OK",
  "values": { "vx": 0.42, "vy": -0.15 },
  "transparency_log_id": "tx-2026-05-14T12:00:00.125Z-0042"
}
```

### Emergency STOP

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

## Limitations

* No per-tick lockstep — both sides run their own clocks. Phase 5 (gRPC
  lockstep, separate spec) addresses this.
* Only the simple 2-D navigation demo signals are mapped in YAML; adding
  new signals requires a YAML edit on the Java side and a `values`-dict
  entry on the Python side.
* Quality is binary (`OK` / `DEGRADED` / `STOP`), not the full
  Jneopallium `Quality` enum. The Java mapper degrades signals to
  `BAD` on `DEGRADED` / `STOP` frames.
* No streaming backpressure across the channel. The frame queue is
  bounded; on overflow the oldest frame is dropped and audited.
* UDS path is local-machine only by design.

## Next step

A gRPC lockstep bridge — separate spec, same frame schema serialized to
Protobuf. Per-tick `step()` synchronisation. Required for any deployment
where Jneopallium and Nengo must agree on simulated time to the
millisecond.
