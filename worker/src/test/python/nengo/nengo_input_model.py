# Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
"""Nengo input model — encodes a scripted 2-D navigation scenario into a
labeled vector and ships one JSONL frame per simulator step to Channel A
(15-NENGO.md §9.2).

This module is intentionally runnable standalone (``python nengo_input_model.py``).
It depends on ``nengo`` and ``numpy`` only; ``run_hybrid_demo.py``
orchestrates input + output together.
"""

from __future__ import annotations

import argparse
import math
import os
import sys
import time

try:
    import nengo
    import numpy as np
except ImportError:  # pragma: no cover - env without nengo
    nengo = None
    np = None

from nengo_frame import (
    NengoFrame,
    SCHEMA_VERSION,
    SOURCE_NENGO_INPUT,
    STATUS_OK,
    now_ms,
    open_channel_a,
)


DEFAULT_CHANNEL_PATH = "/tmp/jneo-nengo-input.sock"
DEFAULT_CHANNEL_MODE = "UDS"
DEFAULT_FRAME_VALID_MS = 250
DEFAULT_TICK_S = 0.01


def scripted_sensor(t: float):
    """A simple scripted trajectory: the agent approaches a goal with a
    moving obstacle and a small but non-zero human-risk signal."""
    dx_target = math.cos(0.5 * t)
    dy_target = math.sin(0.3 * t)
    obstacle_dx = 0.1 * math.sin(2.0 * t)
    obstacle_dy = 0.1 * math.cos(2.0 * t)
    human_risk = 0.05 + 0.02 * math.sin(0.7 * t)
    battery = max(0.0, 1.0 - 0.001 * t)
    return np.array([dx_target, dy_target, obstacle_dx, obstacle_dy,
                     human_risk, battery])


def run(channel_path: str, mode: str, max_steps: int, tick_s: float,
        valid_ms: int) -> int:
    if nengo is None or np is None:
        print("ERROR: nengo and numpy must be installed (see requirements.txt).",
              file=sys.stderr)
        return 2

    model = nengo.Network(label="sensor-encoder")
    with model:
        sensor_input = nengo.Node(lambda t: scripted_sensor(t), size_out=6)
        ens = nengo.Ensemble(n_neurons=600, dimensions=6)
        nengo.Connection(sensor_input, ens)
        decoded = nengo.Probe(ens, synapse=0.01)

    seq = 0
    with nengo.Simulator(model, dt=tick_s, progress_bar=False) as sim, \
            open_channel_a(channel_path, mode) as ch:
        try:
            while seq < max_steps:
                sim.step()
                vec = sim.data[decoded][-1]
                frame = NengoFrame(
                    schema_version=SCHEMA_VERSION,
                    source=SOURCE_NENGO_INPUT,
                    frame_id=f"f-{seq:06d}",
                    sequence_no=seq,
                    timestamp_ms=now_ms(),
                    valid_until_ms=now_ms() + valid_ms,
                    safety_status=STATUS_OK,
                    values={
                        "dx_target":   float(vec[0]),
                        "dy_target":   float(vec[1]),
                        "obstacle_dx": float(vec[2]),
                        "obstacle_dy": float(vec[3]),
                        "human_risk":  float(vec[4]),
                        "battery":     float(vec[5]),
                    },
                )
                ok = True
                payload = frame.to_jsonl()
                if hasattr(ch, "write"):
                    res = ch.write(payload)
                    ok = res is not False
                if not ok:
                    print(f"WARN: failed to write seq={seq}", file=sys.stderr)
                seq += 1
                time.sleep(tick_s)
        except KeyboardInterrupt:
            pass

    return 0


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--channel", default=os.environ.get(
        "JNEO_NENGO_INPUT_PATH", DEFAULT_CHANNEL_PATH))
    p.add_argument("--mode", default=os.environ.get(
        "JNEO_NENGO_MODE", DEFAULT_CHANNEL_MODE), choices=("UDS", "FILE"))
    p.add_argument("--steps", type=int, default=1000,
                   help="number of simulator steps to emit before exit")
    p.add_argument("--tick-s", type=float, default=DEFAULT_TICK_S)
    p.add_argument("--valid-ms", type=int, default=DEFAULT_FRAME_VALID_MS)
    args = p.parse_args(argv)
    return run(args.channel, args.mode, args.steps, args.tick_s, args.valid_ms)


if __name__ == "__main__":
    raise SystemExit(main())
