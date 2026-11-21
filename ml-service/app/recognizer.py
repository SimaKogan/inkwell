"""Handwriting recognition: Claude vision when a key is configured, Tesseract as fallback."""
import base64
import io
import os

from PIL import Image, ImageOps

from .alphabets import detect_language

PROMPT = (
    "Transcribe the handwritten text in this image exactly as written. "
    "Keep the original language(s) and the original line breaks. Do not translate, correct or summarise. "
    "Mark any word you cannot read as [?]. Output only the transcription, nothing else."
)


def prepare(data: bytes, max_side: int = 1568) -> Image.Image:
    img = ImageOps.exif_transpose(Image.open(io.BytesIO(data))).convert("RGB")
    scale = max_side / max(img.size)
    if scale < 1:
        img = img.resize((int(img.width * scale), int(img.height * scale)), Image.LANCZOS)
    return img


def _claude(img: Image.Image) -> str:
    import anthropic

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=90)
    client = anthropic.Anthropic()  # reads ANTHROPIC_API_KEY
    msg = client.messages.create(
        model=os.getenv("ANTHROPIC_MODEL", "claude-sonnet-5-5"),
        max_tokens=2000,
        messages=[{"role": "user", "content": [
            {"type": "image", "source": {"type": "base64", "media_type": "image/jpeg",
                                         "data": base64.b64encode(buf.getvalue()).decode()}},
            {"type": "text", "text": PROMPT},
        ]}],
    )
    return "".join(b.text for b in msg.content if b.type == "text").strip()


def _tesseract(img: Image.Image) -> str:
    import pytesseract

    gray = ImageOps.autocontrast(ImageOps.grayscale(img))
    return pytesseract.image_to_string(gray, lang="heb+rus+eng").strip()


def recognize(data: bytes) -> dict:
    img = prepare(data)
    errors: list[str] = []
    if os.getenv("ANTHROPIC_API_KEY"):
        try:
            text = _claude(img)
            return {"text": text, "language": detect_language(text), "engine": "claude-vision"}
        except Exception as exc:  # noqa: BLE001
            errors.append(f"claude: {exc}")
    try:
        text = _tesseract(img)
        return {"text": text, "language": detect_language(text), "engine": "tesseract"}
    except Exception as exc:  # noqa: BLE001
        errors.append(f"tesseract: {exc}")
    raise RuntimeError("; ".join(errors) or "no recognition engine available")
