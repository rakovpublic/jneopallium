# -*- coding: utf-8 -*-
"""Build every Industrial Loop Guardian deliverable as DOCX.

Run:  python scripts/demo-cybersecurity-training/docgen/build_industrial.py
Output: docs/demo-industrial-fmi/deliverables/*.docx
PDF conversion is performed separately by LibreOffice.
"""

from __future__ import annotations

import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from blocks import render_doc, render_deck                  # noqa: E402
from docbuilder import BLUE, TEAL, NAVY                      # noqa: E402
from content_ind_architecture import architecture           # noqa: E402
from content_ind_training import training                   # noqa: E402
from content_ind_deployment import deployment               # noqa: E402
from content_ind_testreport import testreport               # noqa: E402
from content_ind_pitch import pitch                          # noqa: E402

REPO = HERE.parents[2]
OUT = REPO / "docs" / "demo-industrial-fmi" / "deliverables"
OUT.mkdir(parents=True, exist_ok=True)

FOOTER = {
    "en": "Jneopallium · Industrial Loop Guardian · Supervisory above PLC/PID/SIS · ADVISORY",
    "uk": "Jneopallium · Industrial Loop Guardian · Наглядовий шар над PLC/PID/SIS · ADVISORY",
}

DOCS = [
    ("architecture", architecture, "Architecture-Article", BLUE),
    ("training", training, "Training-Guide", TEAL),
    ("deployment", deployment, "Deployment-Guide", TEAL),
    ("testreport", testreport, "Test-Report", NAVY),
]


def main() -> int:
    built = []
    for _key, fn, name, accent in DOCS:
        for lang in ("en", "uk"):
            builder = render_doc(fn(lang), accent=accent, footer=FOOTER[lang])
            path = OUT / f"Jneopallium-Industrial-LoopGuardian-{name}-{lang.upper()}.docx"
            builder.save(str(path))
            built.append(path)
    for lang in ("en", "uk"):
        deck = render_deck(pitch(lang))
        path = OUT / f"Jneopallium-Industrial-LoopGuardian-Pitch-Deck-{lang.upper()}.docx"
        deck.save(str(path))
        built.append(path)
    print(f"Built {len(built)} DOCX files into {OUT}")
    for p in built:
        print("  -", p.name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
