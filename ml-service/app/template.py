"""Printable handwriting sheet generator."""
import io
from functools import lru_cache

from PIL import Image, ImageDraw, ImageFont

from . import geometry as g
from .alphabets import KEY_TO_ID, SHEETS

FONT_PATHS = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/dejavu/DejaVuSans.ttf",
    "/Library/Fonts/Arial Unicode.ttf",
    "C:/Windows/Fonts/arial.ttf",
]


@lru_cache(maxsize=8)
def ui_font(size: int) -> ImageFont.ImageFont:
    for p in FONT_PATHS:
        try:
            return ImageFont.truetype(p, size)
        except OSError:
            continue
    return ImageFont.load_default()


def marker_bits(sheet_id: int) -> list[int]:
    bits = [(sheet_id >> (3 - i)) & 1 for i in range(4)]
    return bits + [1 - b for b in bits]


def render_sheet(sheet_id: int) -> Image.Image:
    sheet = SHEETS[sheet_id]
    img = Image.new("RGB", (g.PAGE_W, g.PAGE_H), "white")
    d = ImageDraw.Draw(img)

    for cx, cy in g.CORNER_CENTERS:
        h = g.CORNER // 2
        d.rectangle([cx - h, cy - h, cx + h, cy + h], fill="black")

    d.text((g.CORNER_MARGIN + g.CORNER + 24, 70), f"Inkwell handwriting sheet · {sheet['title']}", fill=(20, 28, 50), font=ui_font(34))
    d.text((g.GRID_X, 150), "Write each character in its box, sitting on the blue line. Dark pen, no pencil. "
           "Keep all four black squares visible.", fill=(90, 98, 116), font=ui_font(19))
    for i, bit in enumerate(marker_bits(sheet_id)):
        x = g.MARK_X + i * g.MARK_STEP
        box = [x, g.MARK_Y, x + g.MARK_SIZE, g.MARK_Y + g.MARK_SIZE]
        d.rectangle(box, fill="black" if bit else None, outline=(60, 60, 60), width=2)

    label_font = ui_font(21)
    for idx, ch in enumerate(sheet["chars"]):
        x, y = g.cell_origin(idx)
        d.rectangle([x, y, x + g.CELL_W, y + g.CELL_H], outline=(110, 110, 110), width=2)
        d.text((x + 9, y + 3), ch, fill=(150, 150, 150), font=label_font)
        by = y + g.BASELINE
        d.line([(x + 10, by), (x + g.CELL_W - 10, by)], fill=(170, 190, 255), width=2)
    return img


def build_template(keys: list[str], fmt: str = "pdf") -> tuple[bytes, str]:
    ids = [KEY_TO_ID[k] for k in keys if k in KEY_TO_ID]
    if not ids:
        raise ValueError("Unknown sheet keys; use ru, en, he")
    pages = [render_sheet(i) for i in ids]
    buf = io.BytesIO()
    if fmt == "png":
        pages[0].save(buf, format="PNG", dpi=(200, 200))
        return buf.getvalue(), "image/png"
    pages[0].save(buf, format="PDF", save_all=True, append_images=pages[1:], resolution=200.0)
    return buf.getvalue(), "application/pdf"
