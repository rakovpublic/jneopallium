"""Render recognition-only evidence images from dataset rows and eval predictions.

The Java evaluator writes predictions as JSONL. The bridge writes the matching
full source FPV frame path and detection box into the dataset JSONL. This script
joins them and creates human-inspectable composites: full frame with bounding
box plus a zoomed crop with expected/predicted labels and confidence.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

import cv2
import numpy as np


def read_jsonl(path: Path) -> list[dict]:
    rows: list[dict] = []
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def safe_name(value: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", value)


def box_from(value: object) -> tuple[int, int, int, int] | None:
    if not isinstance(value, dict):
        return None
    try:
        return (
            int(round(float(value["cx"]))),
            int(round(float(value["cy"]))),
            int(round(float(value["w"]))),
            int(round(float(value["h"]))),
        )
    except (KeyError, TypeError, ValueError):
        return None


def bounds_from_box(box: tuple[int, int, int, int], width: int, height: int, pad: float = 0.0) -> tuple[int, int, int, int]:
    cx, cy, bw, bh = box
    half_w = max(1, int(round(bw * (0.5 + pad))))
    half_h = max(1, int(round(bh * (0.5 + pad))))
    x1 = max(0, cx - half_w)
    y1 = max(0, cy - half_h)
    x2 = min(width - 1, cx + half_w)
    y2 = min(height - 1, cy + half_h)
    return x1, y1, x2, y2


def draw_box_and_label(frame: np.ndarray, box: tuple[int, int, int, int], label: str, colour: tuple[int, int, int], scale: float) -> None:
    x1, y1, x2, y2 = bounds_from_box(box, frame.shape[1], frame.shape[0])
    thickness = max(2, int(round(3 * scale)))
    cv2.rectangle(frame, (x1, y1), (x2, y2), colour, thickness)
    font_scale = max(0.55, 0.72 * scale)
    text_size, baseline = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, font_scale, thickness)
    label_y = max(text_size[1] + 8, y1 - 8)
    cv2.rectangle(
        frame,
        (x1, label_y - text_size[1] - 8),
        (min(frame.shape[1] - 1, x1 + text_size[0] + 10), label_y + baseline + 4),
        (18, 18, 18),
        -1,
    )
    cv2.putText(frame, label, (x1 + 5, label_y), cv2.FONT_HERSHEY_SIMPLEX, font_scale, colour, thickness, cv2.LINE_AA)


def image_from_dataset_row(row: dict) -> np.ndarray | None:
    image_path = row.get("imagePath")
    if image_path:
        image = cv2.imread(str(image_path))
        if image is not None:
            return image
    pixels = row.get("pixels")
    if isinstance(pixels, list) and pixels:
        gray = np.array(pixels, dtype=np.uint8)
        return cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
    return None


def make_composite(dataset_row: dict, prediction: dict) -> np.ndarray | None:
    image = image_from_dataset_row(dataset_row)
    if image is None:
        return None
    box = box_from(dataset_row.get("detectionBox") or prediction.get("detectionBox"))
    if box is None:
        return None

    expected = str(prediction.get("expected", dataset_row.get("label", "UNKNOWN")))
    predicted = str(prediction.get("predicted", "UNKNOWN"))
    confidence = float(prediction.get("confidence", 0.0))
    accepted = bool(prediction.get("accepted", False))
    correct = bool(prediction.get("correct", expected == predicted))
    colour = (70, 230, 80) if accepted and correct else ((0, 185, 255) if correct else (40, 40, 240))
    status = "OK" if accepted and correct else ("LOW_CONF" if correct else "WRONG")
    label = f"{status} pred={predicted} conf={confidence:.3f} expected={expected}"

    canvas = np.full((1080, 1920, 3), (30, 32, 34), dtype=np.uint8)
    full = image.copy()
    draw_box_and_label(full, box, label, colour, 1.0)
    full_panel = cv2.resize(full, (1280, 720), interpolation=cv2.INTER_AREA)
    canvas[24:744, 24:1304] = full_panel

    x1, y1, x2, y2 = bounds_from_box(box, image.shape[1], image.shape[0], pad=1.3)
    crop = image[y1:y2, x1:x2]
    if crop.size == 0:
        return None
    crop_panel = cv2.resize(crop, (560, 560), interpolation=cv2.INTER_CUBIC)
    cv2.rectangle(crop_panel, (0, 0), (559, 559), colour, 8)
    canvas[116:676, 1336:1896] = crop_panel

    lines = [
        f"frame: {prediction.get('frameId', dataset_row.get('frameId', 'unknown'))}",
        f"target: {dataset_row.get('trackId', prediction.get('trackId', 'unknown'))}",
        f"predicted: {predicted}",
        f"expected: {expected}",
        f"confidence: {confidence:.3f}",
        f"accepted: {accepted}",
    ]
    y = 804
    for line in lines:
        cv2.putText(canvas, line, (40, y), cv2.FONT_HERSHEY_SIMPLEX, 0.86, (235, 238, 240), 2, cv2.LINE_AA)
        y += 38
    cv2.putText(canvas, "zoomed target crop", (1390, 718), cv2.FONT_HERSHEY_SIMPLEX, 0.8, colour, 2, cv2.LINE_AA)
    return canvas


def write_contact_sheet(images: list[Path], output: Path) -> None:
    if not images:
        return
    thumbs: list[np.ndarray] = []
    for image_path in images[:64]:
        image = cv2.imread(str(image_path))
        if image is None:
            continue
        thumbs.append(cv2.resize(image, (384, 216), interpolation=cv2.INTER_AREA))
    if not thumbs:
        return
    cols = 4
    rows = int(np.ceil(len(thumbs) / cols))
    sheet = np.full((rows * 216, cols * 384, 3), (20, 22, 24), dtype=np.uint8)
    for index, thumb in enumerate(thumbs):
        y = (index // cols) * 216
        x = (index % cols) * 384
        sheet[y:y + 216, x:x + 384] = thumb
    cv2.imwrite(str(output), sheet)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dataset", required=True)
    parser.add_argument("--predictions", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    dataset = {row.get("frameId"): row for row in read_jsonl(Path(args.dataset))}
    predictions = read_jsonl(Path(args.predictions))
    output = Path(args.output).resolve()
    recognized_dir = output / "recognized-boxed-images"
    recognized_dir.mkdir(parents=True, exist_ok=True)

    written: list[Path] = []
    accepted_correct = 0
    wrong = 0
    low_conf = 0
    for prediction in predictions:
        frame_id = prediction.get("frameId")
        dataset_row = dataset.get(frame_id)
        if not dataset_row:
            continue
        composite = make_composite(dataset_row, prediction)
        if composite is None:
            continue
        correct = bool(prediction.get("correct", False))
        accepted = bool(prediction.get("accepted", False))
        if accepted and correct:
            accepted_correct += 1
            prefix = "ok"
        elif correct:
            low_conf += 1
            prefix = "lowconf"
        else:
            wrong += 1
            prefix = "wrong"
        target = safe_name(str(dataset_row.get("trackId", "target")))
        predicted = safe_name(str(prediction.get("predicted", "UNKNOWN")))
        confidence = float(prediction.get("confidence", 0.0))
        path = recognized_dir / f"{prefix}-{target}-{predicted}-{confidence:.3f}-{safe_name(str(frame_id))}.png"
        cv2.imwrite(str(path), composite)
        written.append(path)

    contact_sheet = output / "recognition-evidence-contact-sheet.png"
    write_contact_sheet(written, contact_sheet)
    summary = {
        "dataset": str(Path(args.dataset).resolve()),
        "predictions": str(Path(args.predictions).resolve()),
        "recognizedBoxedImageFolder": str(recognized_dir),
        "contactSheet": str(contact_sheet),
        "imagesWritten": len(written),
        "acceptedCorrect": accepted_correct,
        "correctButBelowThreshold": low_conf,
        "wrong": wrong,
    }
    (output / "recognition-evidence-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
