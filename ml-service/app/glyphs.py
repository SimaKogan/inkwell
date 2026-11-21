"""Turn photographed/scanned handwriting sheets into a portable glyph pack (zip)."""
import io
import itertools
import json
import math
import zipfile

import cv2
import numpy as np
from PIL import Image, ImageOps

from . import geometry as g
from .alphabets import SHEETS
from .template import marker_bits

MAX_VARIANTS = 4
INK_THRESHOLD = 140
MIN_INK_PIXELS = 40


class SheetError(ValueError):
    pass


def load_bgr(data: bytes, max_side: int = 3200) -> np.ndarray:
    try:
        img = ImageOps.exif_transpose(Image.open(io.BytesIO(data))).convert("RGB")
    except Exception as exc:  # noqa: BLE001
        raise SheetError("Не удалось открыть изображение") from exc
    scale = max_side / max(img.size)
    if scale < 1:
        img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
    return cv2.cvtColor(np.asarray(img), cv2.COLOR_RGB2BGR)


def normalized_gray(img: np.ndarray) -> np.ndarray:
    """Flatten uneven lighting: paper ≈ 235, ink stays dark."""
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY).astype(np.float32)
    bg = cv2.GaussianBlur(gray, (0, 0), max(gray.shape) / 25)
    return np.clip(gray / np.maximum(bg, 1) * 235, 0, 255).astype(np.uint8)


def find_corner_squares(img: np.ndarray) -> list[tuple[float, float]]:
    norm = normalized_gray(img)
    # Registration squares are solid black; grid lines and labels are grey, so a strict threshold isolates them.
    _, bw = cv2.threshold(norm, 90, 255, cv2.THRESH_BINARY_INV)
    contours, _ = cv2.findContours(bw, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
    w = img.shape[1]
    cands = []
    for c in contours:
        area = cv2.contourArea(c)
        if area < (w * 0.008) ** 2 or area > (w * 0.12) ** 2:
            continue
        (cx, cy), (rw, rh), _ = cv2.minAreaRect(c)
        if min(rw, rh) < 1:
            continue
        if max(rw, rh) / min(rw, rh) > 1.5 or area / (rw * rh) < 0.8:
            continue
        cands.append((area, (cx, cy)))
    cands.sort(reverse=True)
    cands = cands[:10]
    target = (g.PAGE_W - 2 * (g.CORNER_MARGIN + g.CORNER // 2)) / (g.PAGE_H - 2 * (g.CORNER_MARGIN + g.CORNER // 2))
    best, best_area = None, 0.0
    for combo in itertools.combinations(cands, 4):
        areas = [a for a, _ in combo]
        if max(areas) / min(areas) > 1.8:
            continue
        pts = np.array([p for _, p in combo], dtype=np.float32)
        hull = cv2.convexHull(pts)
        if len(hull) != 4:
            continue
        quad_area = cv2.contourArea(hull)
        sides = [np.linalg.norm(hull[i][0] - hull[(i + 1) % 4][0]) for i in range(4)]
        ratio = (sides[0] + sides[2]) / max(sides[1] + sides[3], 1e-6)
        if not (0.7 < ratio / target < 1.4 or 0.7 < ratio * target < 1.4):
            continue
        if quad_area > best_area:
            best, best_area = pts, quad_area
    if best is None:
        raise SheetError("Не нашёл четыре чёрных квадрата по углам листа. Сфотографируйте лист целиком, при ровном свете.")
    centre = best.mean(axis=0)
    ordered = sorted(best.tolist(), key=lambda p: math.atan2(p[1] - centre[1], p[0] - centre[0]))
    start = min(range(4), key=lambda i: ordered[i][0] + ordered[i][1])
    ordered = ordered[start:] + ordered[:start]
    return [tuple(p) for p in ordered]


def read_sheet_id(page_gray: np.ndarray) -> int | None:
    bits = []
    for i in range(g.MARK_N):
        x = g.MARK_X + i * g.MARK_STEP + g.MARK_SIZE // 2
        y = g.MARK_Y + g.MARK_SIZE // 2
        bits.append(1 if page_gray[y - 8:y + 8, x - 8:x + 8].mean() < 110 else 0)
    value = int("".join(map(str, bits[:4])), 2)
    if value in SHEETS and bits == marker_bits(value):
        return value
    return None


def rectify(img: np.ndarray) -> tuple[np.ndarray, int]:
    corners = find_corner_squares(img)
    dst = np.array(g.CORNER_CENTERS, dtype=np.float32)
    for shift in range(4):
        src = np.array(corners[shift:] + corners[:shift], dtype=np.float32)
        m = cv2.getPerspectiveTransform(src, dst)
        page = cv2.warpPerspective(img, m, (g.PAGE_W, g.PAGE_H), flags=cv2.INTER_CUBIC, borderValue=(255, 255, 255))
        gray = normalized_gray(page)
        sid = read_sheet_id(gray)
        if sid is not None:
            return gray, sid
    raise SheetError("Не распознал номер листа. Используйте шаблон Inkwell и не обрезайте лист.")


def extract_cell(page_gray: np.ndarray, index: int) -> dict | None:
    x, y = g.cell_origin(index)
    x0, x1 = x + 12, x + g.CELL_W - 12
    y0, y1 = y + g.LABEL_H + 6, y + g.CELL_H - 10
    crop = page_gray[y0:y1, x0:x1]
    mask = (crop < INK_THRESHOLD).astype(np.uint8)
    n, labels, stats, _ = cv2.connectedComponentsWithStats(mask, connectivity=8)
    clean = np.zeros_like(mask)
    for i in range(1, n):
        if stats[i, cv2.CC_STAT_AREA] >= 15:
            clean[labels == i] = 1
    if int(clean.sum()) < MIN_INK_PIXELS:
        return None
    ys, xs = np.where(clean > 0)
    bx0, bx1, by0, by1 = max(xs.min() - 2, 0), xs.max() + 3, max(ys.min() - 2, 0), ys.max() + 3
    soft = np.clip((190.0 - crop.astype(np.float32)) * 255.0 / 120.0, 0, 255)
    soft *= cv2.dilate(clean, np.ones((3, 3), np.uint8))
    alpha = soft[by0:by1, bx0:bx1].astype(np.uint8)
    rgba = np.zeros((*alpha.shape, 4), dtype=np.uint8)
    rgba[..., 3] = alpha
    descent = (y0 + by1) - (y + g.BASELINE)
    return {"image": Image.fromarray(rgba, "RGBA"), "descent": int(descent)}


def extract_sheets(images: list[bytes]) -> tuple[bytes, dict]:
    glyphs: dict[str, list[dict]] = {}
    report = []
    expected: set[str] = set()
    for data in images:
        page_gray, sid = rectify(load_bgr(data))
        chars = SHEETS[sid]["chars"]
        expected.update(chars)
        found = []
        for idx, ch in enumerate(chars):
            cell = extract_cell(page_gray, idx)
            if cell is None:
                continue
            bucket = glyphs.setdefault(ch, [])
            if len(bucket) < MAX_VARIANTS:
                bucket.append(cell)
            found.append(ch)
        report.append({"sheet": SHEETS[sid]["key"], "found": len(found), "total": len(chars),
                       "missing": "".join(c for c in chars if c not in found)})
    if not glyphs:
        raise SheetError("На листе не найдено ни одного символа. Пишите тёмной ручкой внутри клеток.")
    buf = io.BytesIO()
    manifest: dict = {"version": 1, "glyphs": {}}
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        for ch, variants in glyphs.items():
            entries = []
            for n, v in enumerate(variants):
                name = f"glyphs/{ord(ch):04x}_{n}.png"
                png = io.BytesIO()
                v["image"].save(png, format="PNG", optimize=True)
                z.writestr(name, png.getvalue())
                entries.append({"f": name, "w": v["image"].width, "h": v["image"].height, "descent": v["descent"]})
            manifest["glyphs"][ch] = entries
        z.writestr("manifest.json", json.dumps(manifest, ensure_ascii=False))
    covered = "".join(sorted(glyphs))
    missing = "".join(sorted(expected - set(glyphs)))
    return buf.getvalue(), {"covered": covered, "missing": missing, "sheets": report}


def load_pack(pack: bytes) -> dict[str, list[dict]]:
    out: dict[str, list[dict]] = {}
    with zipfile.ZipFile(io.BytesIO(pack)) as z:
        manifest = json.loads(z.read("manifest.json"))
        for ch, entries in manifest["glyphs"].items():
            for e in entries:
                img = Image.open(io.BytesIO(z.read(e["f"]))).convert("RGBA")
                out.setdefault(ch, []).append({"image": img, "descent": e["descent"]})
    return out
