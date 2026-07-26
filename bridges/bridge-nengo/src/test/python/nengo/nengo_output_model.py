# Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
"""Nengo output model — reads approved Jneopallium frames from Channel B,
smooths the (vx, vy) motor vector via a Nengo ensemble, and decays to
zero on stale / STOP frames (15-NENGO.md §9.3).

Runnable standalone: ``python nengo_output_model.py``.
"""

from __future__ import annotations

import argparse
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
    STATUS_STOP,
    now_ms,
    open_channel_b,
)


DEFAULT_CHANNEL_PATH = "/tmp/jneo-nengo-output.sock"
DEFAULT_CHANNEL_MODE = "UDS"
DEFAULT_TICK_S = 0.01


def run(channel_path: str, mode: str, max_steps: int, tick_s: float) -> int:
    if nengo is None or np is None:
        print("ERROR: nengo and numpy must be installed (see requirements.txt).",
              file=sys.stderr)
        return 2

    target_holder = {"vec": np.zeros(2), "last_valid_until": 0}

    model = nengo.Network(label="motor-decoder")
    with model:
        target_node = nengo.Node(lambda t: target_holder["vec"], size_out=2)
        motor = nengo.Ensemble(n_neurons=400, dimensions=2)
        nengo.Connection(target_node, motor, synapse=0.05)
        out_probe = nengo.Probe(motor, synapse=0.05)

    step = 0
    with nengo.Simulator(model, dt=tick_s, progress_bar=False) as sim, \
            open_channel_b(channel_path, mode) as ch:
        try:
            while step < max_steps:
                for line in ch.poll_lines(max_lines=32):
                    try:
                        frame = NengoFrame.from_line(line)
                    except Exception as e:
                        print(f"WARN: bad frame: {e}", file=sys.stderr)
                        continue
                    invalid = frame.validate()
                    if invalid is not None:
                        print(f"WARN: rejected frame: {invalid}", file=sys.stderr)
                        continue
                    if frame.safety_status == STATUS_STOP:
                        target_holder["vec"] = np.zeros(2)
                    elif frame.valid_until_ms < now_ms():
                        # stale — ignore
                        continue
                    else:
                        target_holder["vec"] = np.array([
                            float(frame.values.get("vx", 0.0)),
                            float(frame.values.get("vy", 0.0)),
                        ])
                        target_holder["last_valid_until"] = frame.valid_until_ms

                # Watchdog: if no fresh frame, decay target to zero.
                if now_ms() > target_holder["last_valid_until"]:
                    target_holder["vec"] = np.zeros(2)

                sim.step()
                vec = sim.data[out_probe][-1]
                if step % 50 == 0:
                    print(f"motor[{step}] vx={vec[0]: .3f} vy={vec[1]: .3f}")
                step += 1
                time.sleep(tick_s)
        except KeyboardInterrupt:
            pass

    return 0


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--channel", default=os.environ.get(
        "JNEO_NENGO_OUTPUT_PATH", DEFAULT_CHANNEL_PATH))
    p.add_argument("--mode", default=os.environ.get(
        "JNEO_NENGO_MODE", DEFAULT_CHANNEL_MODE), choices=("UDS", "FILE"))
    p.add_argument("--steps", type=int, default=1000)
    p.add_argument("--tick-s", type=float, default=DEFAULT_TICK_S)
    args = p.parse_args(argv)
    return run(args.channel, args.mode, args.steps, args.tick_s)


if __name__ == "__main__":
    raise SystemExit(main())
