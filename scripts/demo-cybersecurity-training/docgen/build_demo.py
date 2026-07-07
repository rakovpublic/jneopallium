# -*- coding: utf-8 -*-
"""Build the industrial demos walkthrough deck as DOCX.

Run:  python scripts/demo-cybersecurity-training/docgen/build_demo.py
Output: docs/demo-materials/deliverables/*.docx  (PDF via LibreOffice separately)
"""

from __future__ import annotations

import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from blocks import render_deck                 # noqa: E402
from content_demo_industrial import demo       # noqa: E402

REPO = HERE.parents[2]
OUT = REPO / "docs" / "demo-materials" / "deliverables"
OUT.mkdir(parents=True, exist_ok=True)


def main() -> int:
    deck = render_deck(demo("en"))
    path = OUT / "Jneopallium-Industrial-Demos-Walkthrough-Deck-EN.docx"
    deck.save(str(path))
    print("Built", path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
