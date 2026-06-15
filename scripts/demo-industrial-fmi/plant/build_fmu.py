from __future__ import annotations

import argparse
from pathlib import Path
import shutil
import subprocess
import sys


def build(output_dir: Path) -> Path:
    here = Path(__file__).resolve().parent
    output_dir.mkdir(parents=True, exist_ok=True)
    target = output_dir / "ThermalSkid.fmu"
    if target.exists():
        target.unlink()

    cmd = [
        sys.executable,
        "-m",
        "pythonfmu",
        "build",
        "-f",
        str(here / "thermal_skid_fmu.py"),
        "-d",
        str(output_dir),
    ]
    subprocess.run(cmd, cwd=here, check=True)

    produced = output_dir / "ThermalSkid.fmu"
    if not produced.exists():
        candidates = list(output_dir.glob("*.fmu"))
        if not candidates:
            raise FileNotFoundError("pythonfmu completed but no .fmu was produced")
        shutil.move(str(candidates[0]), produced)
    return produced


def main() -> int:
    parser = argparse.ArgumentParser(description="Build the ThermalSkid FMI 2.0 Co-Simulation FMU")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path(__file__).resolve().parent / "generated",
    )
    args = parser.parse_args()
    fmu = build(args.output_dir)
    print(fmu)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
