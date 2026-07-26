# Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
"""Orchestrates the input + output Python models for the hybrid Nengo /
Jneopallium demo (15-NENGO.md §9.4).

Starts both models as subprocesses, runs for a fixed duration (default
10 s) or until Ctrl-C, and prints — on each output frame — the matching
expected Jneopallium command so a human can sanity-check the loop
without reading the audit log.
"""

from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--duration-s", type=float, default=10.0)
    p.add_argument("--input-channel", default="/tmp/jneo-nengo-input.sock")
    p.add_argument("--output-channel", default="/tmp/jneo-nengo-output.sock")
    p.add_argument("--mode", default="UDS", choices=("UDS", "FILE"))
    args = p.parse_args(argv)

    env = dict(os.environ)
    env["JNEO_NENGO_INPUT_PATH"] = args.input_channel
    env["JNEO_NENGO_OUTPUT_PATH"] = args.output_channel
    env["JNEO_NENGO_MODE"] = args.mode

    steps = int(args.duration_s / 0.01)

    print(f"[hybrid] mode={args.mode} duration={args.duration_s}s steps={steps}")
    print(f"[hybrid] input  channel: {args.input_channel}")
    print(f"[hybrid] output channel: {args.output_channel}")
    print("[hybrid] Start the Java worker with the Nengo bridge enabled "
          "in another shell first.")

    input_proc = subprocess.Popen(
        [sys.executable, os.path.join(HERE, "nengo_input_model.py"),
         "--steps", str(steps),
         "--channel", args.input_channel,
         "--mode", args.mode],
        env=env,
    )
    output_proc = subprocess.Popen(
        [sys.executable, os.path.join(HERE, "nengo_output_model.py"),
         "--steps", str(steps),
         "--channel", args.output_channel,
         "--mode", args.mode],
        env=env,
    )

    def _stop(*_):
        for proc in (input_proc, output_proc):
            try:
                proc.send_signal(signal.SIGINT)
            except ProcessLookupError:
                pass

    signal.signal(signal.SIGINT, _stop)
    signal.signal(signal.SIGTERM, _stop)

    try:
        t_end = time.time() + args.duration_s + 2.0
        while time.time() < t_end:
            if input_proc.poll() is not None and output_proc.poll() is not None:
                break
            time.sleep(0.1)
    finally:
        _stop()
        for proc in (input_proc, output_proc):
            try:
                proc.wait(timeout=2.0)
            except subprocess.TimeoutExpired:
                proc.kill()

    rc = (input_proc.returncode or 0) | (output_proc.returncode or 0)
    print(f"[hybrid] done; rc={rc}")
    return rc


if __name__ == "__main__":
    raise SystemExit(main())
