"""Block DSL renderers shared by every deliverable.

Content is authored as a flat list of (kind, *payload) tuples so the
English and Ukrainian editions stay structurally parallel and the visual
styling lives entirely in :mod:`docbuilder`.
"""

from __future__ import annotations

from docbuilder import DocBuilder, SlideBuilder, BLUE, TEAL, NAVY


def render_doc(blocks: list, accent=BLUE, footer: str = "") -> DocBuilder:
    b = DocBuilder(accent=accent)
    if footer:
        b.add_footer(footer)
    num_index = 0
    for block in blocks:
        kind = block[0]
        # Reset manual numbering whenever a numbered run is interrupted.
        if kind == "num":
            num_index += 1
        else:
            num_index = 0
        if kind == "cover":
            _, kicker, title, subtitle, meta, doc_type, tagline = block
            b.cover(kicker, title, subtitle, meta, doc_type, tagline)
        elif kind == "toc":
            b.toc(block[1], block[2])
        elif kind == "h1":
            b.h1(block[1], block[2] if len(block) > 2 else None)
        elif kind == "h2":
            b.h2(block[1])
        elif kind == "h3":
            b.h3(block[1])
        elif kind == "p":
            b.para(block[1])
        elif kind == "pi":  # italic paragraph
            b.para(block[1], italic=True, color=TEAL)
        elif kind == "bullet":
            b.bullet(block[1], block[2] if len(block) > 2 else 0)
        elif kind == "num":
            b.numbered(block[1], num_index)
        elif kind == "table":
            widths = block[3] if len(block) > 3 else None
            b.table(block[1], block[2], widths)
        elif kind == "callout":
            b.callout(block[1], block[2], block[3] if len(block) > 3 else "info")
        elif kind == "code":
            b.code(block[1])
        elif kind == "spacer":
            b.spacer(block[1] if len(block) > 1 else 6)
        elif kind == "pagebreak":
            b.page_break()
        else:
            raise ValueError(f"Unknown block kind: {kind}")
    return b


def render_deck(slides: list) -> SlideBuilder:
    s = SlideBuilder()
    for slide in slides:
        kind = slide[0]
        if kind == "cover":
            s.cover_slide(slide[1], slide[2], slide[3], slide[4])
        elif kind == "bullets":
            _, idx, title, lead, items = slide
            s.bullet_slide(idx, title, lead, items)
        elif kind == "metrics":
            _, idx, title, lead, metrics = slide
            s.metric_slide(idx, title, lead, metrics)
        elif kind == "twocol":
            _, idx, title, lh, li, rh, ri = slide
            s.two_col_slide(idx, title, lh, li, rh, ri)
        elif kind == "closing":
            _, title, lines, cta, contact = slide
            s.closing_slide(title, lines, cta, contact)
        else:
            raise ValueError(f"Unknown slide kind: {kind}")
    return s
