from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def render_large_area_recordings(run_dir: Path) -> dict[str, str]:
    """Render videos from actual simulator artifacts produced by the scenario."""
    try:
        from PIL import Image, ImageDraw, ImageFont
        import imageio.v2 as imageio
        import numpy as np
    except ImportError as exc:  # pragma: no cover - exercised only on machines missing optional video deps.
        raise RuntimeError(
            "camera/top-down rendering requires Pillow, imageio, and imageio-ffmpeg"
        ) from exc

    camera_events = _read_jsonl(run_dir / "per-uav-camera-events.jsonl")
    top_down_events = _read_jsonl(run_dir / "top-down-events.jsonl")
    config = json.loads((run_dir / "scenario-config.json").read_text(encoding="utf-8"))
    search_area = config["searchArea"]
    font = ImageFont.load_default()

    camera_frames = [_camera_frame(event, font) for event in camera_events]
    top_down_frames = [_top_down_frame(event, top_down_events, search_area, font) for event in top_down_events]

    camera_gif = run_dir / "camera-video.gif"
    camera_mp4 = run_dir / "camera-video.mp4"
    top_down_gif = run_dir / "top-down-video.gif"
    top_down_mp4 = run_dir / "top-down-video.mp4"
    camera_preview = run_dir / "camera-video-preview.png"
    top_down_preview = run_dir / "top-down-video-preview.png"

    _write_gif(camera_gif, camera_frames)
    _write_gif(top_down_gif, top_down_frames)
    _write_mp4(camera_mp4, camera_frames, imageio, np)
    _write_mp4(top_down_mp4, top_down_frames, imageio, np)
    camera_frames[0].save(camera_preview)
    top_down_frames[min(len(top_down_frames) - 1, max(0, len(top_down_frames) // 2))].save(top_down_preview)
    return {
        "cameraVideoGif": str(camera_gif),
        "cameraVideoMp4": str(camera_mp4),
        "cameraPreview": str(camera_preview),
        "topDownVideoGif": str(top_down_gif),
        "topDownVideoMp4": str(top_down_mp4),
        "topDownPreview": str(top_down_preview),
    }


def _read_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def _write_gif(path: Path, frames: list[Any]) -> None:
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=160, loop=0, optimize=False)


def _write_mp4(path: Path, frames: list[Any], imageio: Any, np: Any) -> None:
    with imageio.get_writer(path, fps=6, codec="libx264", quality=8, macro_block_size=1) as writer:
        for frame in frames:
            for _ in range(3):
                writer.append_data(np.asarray(frame.convert("RGB")))


def _camera_frame(event: dict[str, Any], font: Any) -> Any:
    from PIL import Image, ImageDraw

    width, height = 1280, 720
    image = Image.new("RGB", (width, height), (18, 28, 30))
    draw = ImageDraw.Draw(image)
    _draw_fpv_background(draw, width, height, event.get("simulationTime", 0.0))
    draw.rectangle((0, 0, width, 40), fill=(5, 10, 10))
    draw.rectangle((0, height - 38, width, height), fill=(5, 10, 10))
    draw.text((12, 12), f"{event.get('cameraTopic')} frame={event.get('frameId')}", font=font, fill=(220, 245, 235))
    draw.text((12, height - 26), f"t={event.get('simulationTime', 0.0):.2f}s token={event.get('visualToken', 'SEARCH_SWEEP')}",
              font=font, fill=(220, 245, 235))
    target_model = event.get("targetModel")
    if target_model:
        bbox = event.get("detections", [{}])[0].get("bbox", target_model.get("bbox", [0.38, 0.32, 0.24, 0.22]))
        x0 = int(bbox[0] * width)
        y0 = int(bbox[1] * height)
        x1 = int((bbox[0] + bbox[2]) * width)
        y1 = int((bbox[1] + bbox[3]) * height)
        if target_model.get("classLabel") == "infantry":
            _draw_detailed_infantry(draw, (x0, y0, x1, y1))
            color = (115, 255, 170)
        else:
            _draw_detailed_vehicle(draw, (x0, y0, x1, y1))
            color = (120, 205, 255)
        detection = event.get("detections", [{}])[0]
        label = detection.get("classLabel", target_model.get("classLabel"))
        confidence = detection.get("confidence", 0.0)
        draw.rectangle((x0, y0, x1, y1), outline=color, width=4)
        draw.rectangle((x0, max(42, y0 - 25), x0 + 320, max(65, y0)), fill=(5, 12, 9))
        draw.text((x0 + 8, max(46, y0 - 20)), f"{label} conf={confidence:.2f}", font=font, fill=(230, 255, 235))
    else:
        draw.text((width // 2 - 70, height // 2 + 42), "search sweep", font=font, fill=(150, 185, 170))
    return image


def _draw_fpv_background(draw: Any, width: int, height: int, simulation_time: float) -> None:
    for y in range(height):
        sky = y < height * 0.45
        if sky:
            color = (30 + y // 40, 46 + y // 30, 54 + y // 25)
        else:
            color = (35 + y // 55, 55 + y // 65, 38 + y // 90)
        draw.line((0, y, width, y), fill=color)
    horizon = int(height * 0.45)
    draw.line((0, horizon, width, horizon), fill=(120, 145, 115), width=2)
    offset = int((simulation_time * 17) % 80)
    for x in range(-80 + offset, width + 80, 80):
        draw.line((x, horizon, width // 2 + (x - width // 2) // 2, height), fill=(44, 76, 55), width=1)
    for y in range(int(height * 0.55), height, 58):
        draw.arc((120, y - 300, width - 120, y + 300), 0, 180, fill=(44, 76, 55), width=1)
    cx, cy = width // 2, height // 2
    draw.line((cx - 42, cy, cx - 10, cy), fill=(190, 235, 210), width=1)
    draw.line((cx + 10, cy, cx + 42, cy), fill=(190, 235, 210), width=1)
    draw.line((cx, cy - 34, cx, cy - 10), fill=(190, 235, 210), width=1)
    draw.line((cx, cy + 10, cx, cy + 34), fill=(190, 235, 210), width=1)


def _draw_detailed_infantry(draw: Any, box: tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    cx = (x0 + x1) // 2
    helmet = max(8, w // 8)
    torso_top = y0 + int(h * 0.24)
    torso_bottom = y0 + int(h * 0.62)
    suit = (96, 126, 94)
    edge = (220, 235, 210)
    draw.ellipse((cx - helmet, y0 + 8, cx + helmet, y0 + 8 + helmet * 2), fill=(118, 138, 105), outline=edge, width=2)
    draw.polygon([(cx - w * 0.18, torso_top), (cx + w * 0.18, torso_top), (cx + w * 0.12, torso_bottom),
                  (cx - w * 0.12, torso_bottom)], fill=suit, outline=edge)
    draw.rectangle((cx + int(w * 0.16), torso_top + 8, cx + int(w * 0.32), torso_bottom - 6),
                   fill=(70, 96, 72), outline=edge)
    draw.line((cx - int(w * 0.16), torso_top + 10, x0 + int(w * 0.08), y0 + int(h * 0.54)), fill=edge, width=4)
    draw.line((cx + int(w * 0.16), torso_top + 10, x1 - int(w * 0.08), y0 + int(h * 0.54)), fill=edge, width=4)
    draw.line((cx - int(w * 0.08), torso_bottom, x0 + int(w * 0.20), y1 - 8), fill=edge, width=4)
    draw.line((cx + int(w * 0.08), torso_bottom, x1 - int(w * 0.20), y1 - 8), fill=edge, width=4)
    draw.line((x0 + int(w * 0.20), y1 - 8, x0 + int(w * 0.38), y1 - 8), fill=edge, width=3)
    draw.line((x1 - int(w * 0.20), y1 - 8, x1 - int(w * 0.38), y1 - 8), fill=edge, width=3)


def _draw_detailed_vehicle(draw: Any, box: tuple[int, int, int, int]) -> None:
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    body = (130, 145, 122)
    edge = (230, 245, 218)
    draw.rounded_rectangle((x0 + 10, y0 + int(h * 0.30), x1 - 10, y1 - int(h * 0.18)),
                           radius=16, fill=body, outline=edge, width=3)
    draw.rectangle((x0 + int(w * 0.30), y0 + int(h * 0.12), x1 - int(w * 0.30), y0 + int(h * 0.40)),
                   fill=(105, 130, 112), outline=edge, width=2)
    draw.rectangle((x0 + int(w * 0.36), y0 + int(h * 0.17), x1 - int(w * 0.36), y0 + int(h * 0.34)),
                   fill=(45, 62, 63), outline=edge)
    for wx in (x0 + int(w * 0.18), x0 + int(w * 0.70)):
        draw.ellipse((wx, y1 - int(h * 0.28), wx + int(w * 0.14), y1 - int(h * 0.02)),
                     fill=(12, 16, 15), outline=edge, width=2)
    draw.line((x0 + int(w * 0.16), y0 + int(h * 0.48), x1 - int(w * 0.16), y0 + int(h * 0.48)),
              fill=(185, 205, 165), width=2)


def _top_down_frame(event: dict[str, Any], events: list[dict[str, Any]], search_area: dict[str, float], font: Any) -> Any:
    from PIL import Image, ImageDraw

    width, height = 1280, 720
    margin = 70
    image = Image.new("RGB", (width, height), (18, 24, 26))
    draw = ImageDraw.Draw(image)
    draw.rectangle((margin, margin, width - margin, height - margin), outline=(120, 145, 125), width=2)
    for frac in [0.25, 0.5, 0.75]:
        x = margin + int((width - 2 * margin) * frac)
        y = margin + int((height - 2 * margin) * frac)
        draw.line((x, margin, x, height - margin), fill=(42, 65, 55))
        draw.line((margin, y, width - margin, y), fill=(42, 65, 55))

    history = [item for item in events if item.get("sequence", 0) <= event.get("sequence", 0)]
    points = [_map_point(item["uavPose"]["x"], item["uavPose"]["y"], search_area, width, height, margin)
              for item in history if "uavPose" in item]
    for a, b in zip(points, points[1:]):
        draw.line((a[0], a[1], b[0], b[1]), fill=(95, 180, 230), width=3)

    for target in event.get("targets", []):
        x, y = _map_point(target["x"], target["y"], search_area, width, height, margin)
        color = (110, 255, 170) if target["classLabel"] == "infantry" else (130, 200, 255)
        outline = color if target.get("active", True) else (100, 110, 105)
        draw.ellipse((x - 9, y - 9, x + 9, y + 9), fill=outline, outline=(240, 255, 235), width=2)
        draw.text((x + 12, y - 8), target["classLabel"], font=font, fill=outline)

    ux, uy = _map_point(event["uavPose"]["x"], event["uavPose"]["y"], search_area, width, height, margin)
    draw.polygon([(ux, uy - 13), (ux + 11, uy + 10), (ux, uy + 5), (ux - 11, uy + 10)], fill=(255, 245, 140))
    radius = event.get("detectionRadiusMeters", 0.0)
    if radius:
        sx = (width - 2 * margin) / max(1.0, search_area["maxX"] - search_area["minX"])
        sy = (height - 2 * margin) / max(1.0, search_area["maxY"] - search_area["minY"])
        rr = int(radius * min(sx, sy))
        draw.ellipse((ux - rr, uy - rr, ux + rr, uy + rr), outline=(200, 220, 120), width=1)

    draw.rectangle((0, 0, width, 42), fill=(5, 10, 10))
    draw.rectangle((0, height - 38, width, height), fill=(5, 10, 10))
    draw.text((12, 12), f"large-area search top-down t={event.get('simulationTime', 0.0):.2f}s event={event.get('event')}",
              font=font, fill=(230, 250, 235))
    draw.text((12, height - 26), f"pose=({event['uavPose']['x']:.0f},{event['uavPose']['y']:.0f}) waypoint={event.get('waypointIndex', '-')}",
              font=font, fill=(230, 250, 235))
    return image


def _map_point(x: float, y: float, area: dict[str, float], width: int, height: int, margin: int) -> tuple[int, int]:
    px = margin + int((x - area["minX"]) / max(1.0, area["maxX"] - area["minX"]) * (width - 2 * margin))
    py = height - margin - int((y - area["minY"]) / max(1.0, area["maxY"] - area["minY"]) * (height - 2 * margin))
    return px, py
