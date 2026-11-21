"""End-to-end test: print template -> 'write' on it -> photograph (perspective + noise) -> extract -> render."""
import io

import cv2
import numpy as np
from fastapi.testclient import TestClient
from PIL import Image, ImageDraw, ImageFont

from app import geometry as g
from app.alphabets import SHEETS, detect_language
from app.glyphs import extract_sheets
from app.main import app
from app.render import render_text
from app.template import FONT_PATHS, render_sheet


def _font(size):
    for p in FONT_PATHS:
        try:
            return ImageFont.truetype(p.replace("DejaVuSans.ttf", "DejaVuSans-Oblique.ttf"), size)
        except OSError:
            continue
    return ImageFont.truetype(FONT_PATHS[0], size)


def fill_and_photograph(sheet_id: int, skip: str = "") -> bytes:
    page = render_sheet(sheet_id)
    d = ImageDraw.Draw(page)
    f = _font(84)
    for idx, ch in enumerate(SHEETS[sheet_id]["chars"]):
        if ch in skip:
            continue
        x, y = g.cell_origin(idx)
        d.text((x + 45, y + g.BASELINE), ch, fill=(20, 30, 110), font=f, anchor="ls")
    arr = np.asarray(page)[:, :, ::-1].copy()
    # "photo": shrink, perspective-skew, tilt on a dark desk, uneven light, noise
    h, w = arr.shape[:2]
    src = np.float32([[0, 0], [w, 0], [w, h], [0, h]])
    dst = np.float32([[140, 90], [1500, 40], [1560, 2050], [60, 2150]])
    m = cv2.getPerspectiveTransform(src, dst)
    canvas = np.full((2300, 1700, 3), 60, np.uint8)
    warped = cv2.warpPerspective(arr, m, (1700, 2300), dst=canvas, borderMode=cv2.BORDER_TRANSPARENT)
    grad = np.linspace(0.75, 1.05, 1700, dtype=np.float32)[None, :, None]
    shot = np.clip(warped.astype(np.float32) * grad + np.random.default_rng(1).normal(0, 4, warped.shape), 0, 255).astype(np.uint8)
    ok, buf = cv2.imencode(".jpg", shot, [cv2.IMWRITE_JPEG_QUALITY, 80])
    assert ok
    return buf.tobytes()


def test_extract_from_skewed_photo():
    pack, report = extract_sheets([fill_and_photograph(0, skip="Ж")])
    ru_chars = SHEETS[0]["chars"]
    assert "Ж" in report["missing"]
    assert len(report["covered"]) >= len(set(ru_chars)) - 4
    assert {"а", "я", "Б", "5", "."} <= set(report["covered"])


def test_hebrew_sheet_and_rotation():
    data = fill_and_photograph(2)
    img = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)
    rotated = cv2.rotate(img, cv2.ROTATE_180)
    ok, buf = cv2.imencode(".jpg", rotated)
    pack, report = extract_sheets([buf.tobytes()])
    assert "ש" in report["covered"] and "ם" in report["covered"]


def test_render_ltr_and_rtl_and_variants():
    pack, _ = extract_sheets([fill_and_photograph(0), fill_and_photograph(1), fill_and_photograph(2)])
    img, missing = render_text(pack, "Привет, мир! Hello 2026", seed=3)
    assert img.width == 1200 and img.height > 150 and missing == ""
    assert np.asarray(img)[:, :, :3].min() < 120      # ink was actually drawn
    he, miss_he = render_text(pack, "שלום עולם 123", seed=3)
    assert he.height > 100
    _, miss = render_text(pack, "漢字", seed=1)
    assert set(miss) == {"漢", "字"}
    tr, _ = render_text(pack, "ok", paper="transparent", seed=1)
    assert np.asarray(tr)[:, :, 3].max() > 0


def test_http_endpoints():
    c = TestClient(app)
    assert c.get("/health").status_code == 200
    r = c.get("/template?sheets=ru,he&format=pdf")
    assert r.status_code == 200 and r.content[:4] == b"%PDF"
    r = c.post("/glyphs/extract", files=[("sheets", ("a.jpg", fill_and_photograph(1), "image/jpeg"))])
    assert r.status_code == 200, r.text
    body = r.json()
    r2 = c.post("/glyphs/render", json={"packB64": body["packB64"], "text": "Hello", "seed": 1})
    assert r2.status_code == 200 and r2.json()["missing"] == ""
    bad = c.post("/glyphs/extract", files=[("sheets", ("x.png", io.BytesIO(b"not an image"), "image/png"))])
    assert bad.status_code == 422


def test_language_detection():
    assert detect_language("שלום עולם") == "he"
    assert detect_language("привет мир") == "ru"
    assert detect_language("hello") == "en"
    assert detect_language("hello привет") == "mixed"
