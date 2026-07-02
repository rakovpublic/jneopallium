# -*- coding: utf-8 -*-
"""Build every Self-Supervised Maintenance Guardian deliverable as DOCX.

Run:  python scripts/demo-cybersecurity-training/docgen/build_ssm.py
Output: docs/self-supervised-maintenance/deliverables/*.docx
PDF conversion is performed separately by LibreOffice.
"""

from __future__ import annotations

import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from blocks import render_doc, render_deck              # noqa: E402
from docbuilder import BLUE, TEAL, NAVY                  # noqa: E402
from content_ssm_architecture import architecture       # noqa: E402
from content_ssm_training import training               # noqa: E402
from content_ssm_deployment import deployment           # noqa: E402
from content_ssm_testreport import testreport           # noqa: E402
from content_ssm_pitch import pitch                       # noqa: E402

REPO = HERE.parents[2]
OUT = REPO / "docs" / "self-supervised-maintenance" / "deliverables"
OUT.mkdir(parents=True, exist_ok=True)

FOOTER = {
    "en": "Jneopallium · Self-Supervised Maintenance Guardian · Label-free predictive maintenance · ADVISORY",
    "uk": "Jneopallium · Self-Supervised Maintenance Guardian · Прогнозне обслуговування без міток · ADVISORY",
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
            path = OUT / f"Jneopallium-SelfSupervised-Maintenance-{name}-{lang.upper()}.docx"
            builder.save(str(path))
            built.append(path)
    for lang in ("en", "uk"):
        deck = render_deck(pitch(lang))
        path = OUT / f"Jneopallium-SelfSupervised-Maintenance-Pitch-Deck-{lang.upper()}.docx"
        deck.save(str(path))
        built.append(path)
    print(f"Built {len(built)} DOCX files into {OUT}")
    for p in built:
        print("  -", p.name)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
