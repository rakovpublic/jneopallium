#!/usr/bin/env python3
import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ad_fraud_pipeline import TARGET, build_examples, write_jsonl

if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--max-rows", type=int, default=380)
    p.add_argument("--seed", type=int, default=1729)
    args = p.parse_args()
    rows = []
    for example in build_examples(args.max_rows, args.seed):
        rows.append({**example.event, **{f"label_{k}": v for k, v in example.labels.items()}, "split": example.split})
    write_jsonl(TARGET / "synthetic_ad_fraud_events.jsonl", rows)
