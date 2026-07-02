"""Synthetic multi-asset, multi-regime pump telemetry (pure stdlib).

Used for two things only:
  * initial training  -> healthy-only frames feed the label-free fit;
  * tests             -> optionally inject degradations whose timing is kept
                         in an oracle purely to score detection.

No fault label is ever exposed to training. The generator physically couples
sensors (power tracks flow, bearing temp tracks vibration, ...) so the
cross-sensor reconstruction has real structure to learn.
"""

from __future__ import annotations

import math
import random

SENSORS = [
    "suction_pressure",
    "flow",
    "pump_speed",
    "pump_power",
    "vibration_rms",
    "bearing_temp",
    "process_temp",
    "valve_position",
]
REGIMES = 3


def regime_at(tick: int, phase: int) -> int:
    cycle = (tick + phase) % 900
    if cycle < 360:
        return 0
    if cycle < 660:
        return 1
    return 2


def healthy_sample(rng: random.Random, regime: int) -> dict:
    flow = 50.0 + 22.0 * regime + rng.gauss(0, 1.4)
    speed = 1180.0 + 280.0 * regime + rng.gauss(0, 14.0)
    power = 5.0 + 0.42 * flow + 0.004 * speed + rng.gauss(0, 0.5)
    suction = 3.1 - 0.08 * regime + rng.gauss(0, 0.05)
    vibration = 0.9 + 0.18 * regime + 0.04 * (power - 25.0) + rng.gauss(0, 0.06)
    bearing = 39.0 + 4.5 * regime + 1.6 * vibration + rng.gauss(0, 0.4)
    process_t = 58.0 + 7.5 * regime + 0.25 * power + rng.gauss(0, 0.4)
    valve = 28.0 + 14.0 * regime + rng.gauss(0, 0.8)
    return {
        "suction_pressure": suction,
        "flow": flow,
        "pump_speed": speed,
        "pump_power": power,
        "vibration_rms": vibration,
        "bearing_temp": bearing,
        "process_temp": process_t,
        "valve_position": valve,
    }


def apply_fault(row: dict, family: str, p: float, tick: int, rng: random.Random) -> None:
    """Degradation of progress p in [0,1]. Eval-only; never used in training."""
    if family == "bearing_wear":
        row["vibration_rms"] += 1.7 * p
        row["bearing_temp"] += 9.0 * p
        row["pump_power"] += 2.5 * p
    elif family == "cavitation":
        row["suction_pressure"] -= 1.3 * p
        row["vibration_rms"] += 1.1 * p
        row["flow"] += rng.gauss(0, 2.5 * p)
    elif family == "sensor_drift":
        row["process_temp"] += 7.0 * p
    elif family == "energy_drift":
        row["pump_power"] += 4.5 * p
    elif family == "oscillation":
        amp = 4.0 * p
        osc = amp * math.sin(tick / 5.5)
        row["valve_position"] += osc
        row["flow"] += 0.6 * osc


def generate(seed: int, roster, ticks: int, warmup: int, with_faults: bool):
    """Return (data, oracle).

    roster: list of (assetId, family) where family == "healthy" or a fault name.
    data[asset]   = list of per-tick dicts (sensors + "regime")
    oracle[asset] = {"family", "onset"|None, "event"|None}
    """
    data, oracle = {}, {}
    for ai, (asset, family) in enumerate(roster):
        rng = random.Random(seed + 101 * (ai + 1))
        phase = rng.randint(0, 899)
        onset = event = None
        if with_faults and family != "healthy":
            onset = rng.randint(warmup + 200, warmup + 850)
            event = min(onset + rng.randint(700, 1200), ticks - 1)
        series = []
        for t in range(ticks):
            regime = regime_at(t, phase)
            row = healthy_sample(rng, regime)
            if onset is not None and t >= onset:
                p = min(1.0, (t - onset) / max(1, (event - onset)))
                apply_fault(row, family, p, t, rng)
            row["regime"] = regime
            series.append(row)
        data[asset] = series
        oracle[asset] = {"family": family, "onset": onset, "event": event}
    return data, oracle
