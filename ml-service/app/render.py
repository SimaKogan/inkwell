"""Compose text in the user's own handwriting from a glyph pack."""
import random

from PIL import Image, ImageDraw

from .alphabets import detect_language, is_hebrew
from .glyphs import load_pack

REF = 100.0            # glyph pixels are stored at ~200 dpi; size=100 draws them 1:1
FALLBACKS = {"—": "-", "«": '"', "»": '"', "ё": "е", "Ё": "Е", "’": "'", "“": '"', "”": '"', "–": "-", "…": "."}


def _hex(color: str) -> tuple[int, int, int]:
    c = color.lstrip("#")
    if len(c) != 6:
        return (27, 42, 107)
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))  # type: ignore[return-value]


def _find(pack: dict, ch: str):
    """Return (variants, scale_multiplier) for a character, with graceful fallbacks."""
    if ch in pack:
        return pack[ch], 1.0
    alt = FALLBACKS.get(ch)
    if alt and alt in pack:
        return pack[alt], 1.0
    if ch.isalpha() and ch.swapcase() in pack:
        return pack[ch.swapcase()], 0.8 if ch.isupper() else 1.25
    return None, 1.0


def _tint(glyph: Image.Image, rgb: tuple[int, int, int]) -> Image.Image:
    solid = Image.new("RGBA", glyph.size, rgb + (0,))
    solid.putalpha(glyph.getchannel("A"))
    return solid


def _blit(layer: Image.Image, glyph: Image.Image, x: int, y: int) -> None:
    left, top = max(0, -x), max(0, -y)
    right, bottom = min(glyph.width, layer.width - x), min(glyph.height, layer.height - y)
    if right <= left or bottom <= top:
        return
    part = glyph.crop((left, top, right, bottom))
    region = layer.crop((x + left, y + top, x + right, y + bottom))
    layer.paste(Image.alpha_composite(region, part), (x + left, y + top))


def render_text(pack_bytes: bytes, text: str, paper: str = "ruled", ink: str = "#1b2a6b",
                size: int = 64, width: int = 1200, seed: int | None = None) -> tuple[Image.Image, str]:
    pack = load_pack(pack_bytes)
    rng = random.Random(seed)
    scale = max(20, min(size, 160)) / REF
    width = max(600, min(width, 2400))
    margin = int(70 * (size / 64))
    line_h = int(size * 1.75)
    gap = size * 0.02
    space = size * 0.38
    rgb = _hex(ink)
    missing: set[str] = set()

    rtl_doc = detect_language(text) == "he"
    lines: list[tuple[bool, list[tuple[Image.Image, float, float]]]] = []

    def make_glyph(ch: str):
        variants, mult = _find(pack, ch)
        if variants is None:
            missing.add(ch)
            return None
        v = rng.choice(variants)
        s = scale * mult * rng.uniform(0.97, 1.03)
        img = v["image"].resize((max(1, int(v["image"].width * s)), max(1, int(v["image"].height * s))), Image.LANCZOS)
        img = img.rotate(rng.gauss(0, 1.2), resample=Image.BICUBIC, expand=True)
        dy = v["descent"] * s * mult - img.height + rng.gauss(0, 1.2)
        return _tint(img, rgb), dy

    def word_glyphs(word: str):
        rtl = any(is_hebrew(c) for c in word)
        items = [make_glyph(c) for c in word]
        items = [i for i in items if i]
        return items, rtl

    usable = width - 2 * margin
    for paragraph in text.split("\n"):
        words = [w for w in paragraph.split(" ") if w]
        para_rtl = detect_language(paragraph) == "he"
        line: list[tuple[Image.Image, float, float]] = []
        cursor = 0.0
        for w in words:
            seq, word_rtl = word_glyphs(w)
            w_width = sum(i[0].width + gap for i in seq)
            if cursor > 0 and cursor + w_width > usable:
                lines.append((para_rtl, line))
                line, cursor = [], 0.0
            # Lay out left-to-right; the whole line is mirrored later for Hebrew documents.
            # A word whose direction differs from the document's is reversed so it reads correctly.
            n = len(seq)
            order = reversed(range(n)) if word_rtl != para_rtl else range(n)
            xs = [0.0] * n
            acc = cursor
            for k in order:
                xs[k] = acc
                acc += seq[k][0].width + gap
            for k, (glyph, dy) in enumerate(seq):
                line.append((glyph, xs[k], dy))
            cursor += w_width + space
        lines.append((para_rtl, line))

    height = int(margin * 0.9 + max(1, len(lines)) * line_h + margin * 0.6)
    ink_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for n, (line_rtl, line) in enumerate(lines):
        base = margin * 0.9 + (n + 1) * line_h - size * 0.35 + rng.gauss(0, 1.5)
        slope = rng.uniform(-0.004, 0.004)
        for glyph, x, dy in line:
            gx = int(width - margin - x - glyph.width) if line_rtl else int(margin + x)
            gy = int(base + dy + slope * x)
            _blit(ink_layer, glyph, gx, gy)

    if paper == "transparent":
        return ink_layer, "".join(sorted(missing))
    page = Image.new("RGBA", (width, height), (250, 250, 246, 255))
    if paper == "ruled":
        d = ImageDraw.Draw(page)
        for n in range(len(lines)):
            y = int(margin * 0.9 + (n + 1) * line_h - size * 0.35 + size * 0.18)
            d.line([(30, y), (width - 30, y)], fill=(176, 196, 240, 255), width=2)
        mx = width - margin + 30 if rtl_doc else margin - 30
        d.line([(mx, 0), (mx, height)], fill=(232, 120, 120, 255), width=2)
    return Image.alpha_composite(page, ink_layer), "".join(sorted(missing))
