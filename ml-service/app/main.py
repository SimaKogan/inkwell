import base64
import logging
import os

from fastapi import FastAPI, File, Form, HTTPException, Query, UploadFile
from fastapi.responses import Response
from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel

from . import answer as answer_mod
from . import embeddings, recognizer
from .glyphs import SheetError, extract_sheets
from .render import render_text
from .template import build_template

log = logging.getLogger("inkwell.ml")
app = FastAPI(title="Inkwell ML service", version="1.0.0")


class Camel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class EmbedRequest(Camel):
    texts: list[str] = Field(min_length=1, max_length=64)


class RenderRequest(Camel):
    pack_b64: str
    text: str = Field(min_length=1, max_length=2000)
    paper: str = "ruled"
    ink_color: str = "#1b2a6b"
    size: int = 64
    width: int = 1200
    seed: int | None = None


class Source(Camel):
    n: int
    title: str
    text: str


class AnswerRequest(Camel):
    query: str = Field(min_length=1, max_length=500)
    sources: list[Source] = Field(min_length=1, max_length=8)


@app.get("/health")
def health():
    return {"status": "ok", "claude": bool(os.getenv("ANTHROPIC_API_KEY"))}


@app.post("/embed")
def embed(req: EmbedRequest):
    try:
        vectors = embeddings.embed(req.texts)
    except Exception as exc:  # noqa: BLE001
        log.exception("embedding failed")
        raise HTTPException(503, f"Embedding model unavailable: {exc}") from exc
    return {"vectors": vectors, "dim": len(vectors[0])}


@app.post("/recognize")
async def recognize(image: UploadFile = File(...)):
    data = await image.read()
    if not data:
        raise HTTPException(422, "Пустой файл")
    try:
        return recognizer.recognize(data)
    except RuntimeError as exc:
        raise HTTPException(503, f"Нет доступного движка распознавания: {exc}") from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(422, f"Не удалось обработать изображение: {exc}") from exc


@app.get("/template")
def template(sheets: str = Query("ru,en,he"), format: str = Query("pdf", pattern="^(pdf|png)$")):
    try:
        body, mime = build_template([s.strip() for s in sheets.split(",")], format)
    except ValueError as exc:
        raise HTTPException(422, str(exc)) from exc
    return Response(body, media_type=mime)


@app.post("/glyphs/extract")
async def glyphs_extract(sheets: list[UploadFile] = File(...)):
    images = [await f.read() for f in sheets]
    try:
        pack, report = extract_sheets(images)
    except SheetError as exc:
        raise HTTPException(422, str(exc)) from exc
    return {"packB64": base64.b64encode(pack).decode(), **report}


@app.post("/glyphs/render")
def glyphs_render(req: RenderRequest):
    import io

    try:
        pack = base64.b64decode(req.pack_b64)
        img, missing = render_text(pack, req.text, req.paper, req.ink_color, req.size, req.width, req.seed)
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(422, f"Не удалось сгенерировать текст: {exc}") from exc
    buf = io.BytesIO()
    img.save(buf, format="PNG", optimize=True)
    return {"imageB64": base64.b64encode(buf.getvalue()).decode(), "width": img.width,
            "height": img.height, "missing": missing}


@app.post("/answer")
def answer(req: AnswerRequest):
    if not os.getenv("ANTHROPIC_API_KEY"):
        raise HTTPException(503, "ANTHROPIC_API_KEY не задан")
    try:
        return {"answer": answer_mod.answer(req.query, [s.model_dump() for s in req.sources])}
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(502, f"LLM error: {exc}") from exc
