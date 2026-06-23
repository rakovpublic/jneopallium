#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ad_fraud_pipeline import discover_sources

if __name__ == "__main__":
    discover_sources()
