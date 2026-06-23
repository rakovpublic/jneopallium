#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ad_fraud_pipeline import build_examples, write_jsonl, TARGET

if __name__ == "__main__":
    examples = build_examples(380, 1729)
    write_jsonl(TARGET / "normalized_events.jsonl", [{**e.event, "split": e.split} for e in examples])
