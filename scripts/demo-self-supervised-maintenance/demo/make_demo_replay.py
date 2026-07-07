"""Generate a deterministic telemetry replay for the live demo.

Python only generates the data (as it does for tests). The real Java neurons in
SelfSupervisedMaintenanceDemo replay it and do all the detection and learning.

The replay contains two assets over the same window:
  * PUMP-101 — healthy, but with a few benign process excursions that look
    anomalous (the raw material the feedback loop learns to suppress);
  * PUMP-102 — a slow bearing-wear degradation.

Fault/disturbance timings are written to replay-oracle.json purely so the demo
narration and scoring can reference ground truth; the model never sees them.

Run:  python make_demo_replay.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent                      # scripts/demo-self-supervised-maintenance
sys.path.insert(0, str(ROOT))

import synth_telemetry as st            # noqa: E402
from synth_telemetry import SENSORS     # noqa: E402

SEED = 4242
TICKS = 1800
WARMUP = 600                            # skip: the model is already trained
OUT_DIR = Path(__file__).resolve().parents[3] / "target" / "jneopallium-ss-maintenance-demo"

# A long, slow bearing-wear ramp on PUMP-102 (deterministic, so the demo is
# repeatable): its evidence climbs well clear of the nuisance level.
FAULT_ONSET = 800
FAULT_EVENT = 1700
# Sustained but harmless excursions on the healthy PUMP-101 (60-tick windows):
# long enough to trip a naive threshold, but an operator confirms they are benign.
DISTURBANCES = [900, 1250]
DISTURB_LEN = 60


def main() -> int:
    # both assets start healthy; we inject the ramp and the excursions ourselves
    roster = [("PUMP-101", "healthy"), ("PUMP-102", "healthy")]
    data, _ = st.generate(SEED, roster, TICKS, WARMUP, with_faults=False)

    # PUMP-102: deterministic bearing-wear degradation
    import random
    rng = random.Random(SEED + 7)
    for t in range(FAULT_ONSET, TICKS):
        p = min(1.0, (t - FAULT_ONSET) / (FAULT_EVENT - FAULT_ONSET))
        st.apply_fault(data["PUMP-102"][t], "bearing_wear", p, t, rng)

    # PUMP-101: benign, harmless excursions
    for start in DISTURBANCES:
        for t in range(start, start + DISTURB_LEN):
            row = data["PUMP-101"][t]
            row["vibration_rms"] += 0.55
            row["pump_power"] += 1.2
            row["bearing_temp"] += 0.8

    frames = []
    for t in range(WARMUP, TICKS):
        for asset in ("PUMP-101", "PUMP-102"):
            row = data[asset][t]
            frames.append({
                "asset": asset,
                "tick": t,
                "regime": row["regime"],
                "sensors": {s: round(row[s], 5) for s in SENSORS},
            })

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "replay.json").write_text(json.dumps(frames), encoding="utf-8")
    oracle_out = {
        "PUMP-101": {"family": "healthy", "benignDisturbances": DISTURBANCES,
                     "disturbanceLength": DISTURB_LEN},
        "PUMP-102": {"family": "bearing_wear", "onset": FAULT_ONSET, "event": FAULT_EVENT},
        "window": {"from": WARMUP, "to": TICKS - 1},
    }
    (OUT_DIR / "replay-oracle.json").write_text(json.dumps(oracle_out, indent=2), encoding="utf-8")

    print(f"Wrote {len(frames)} frames to {OUT_DIR / 'replay.json'}")
    print(f"PUMP-102 bearing wear: onset={FAULT_ONSET} event={FAULT_EVENT}")
    print(f"PUMP-101 benign excursions at ticks {DISTURBANCES} (len {DISTURB_LEN})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
