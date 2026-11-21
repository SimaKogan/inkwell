import os
from functools import lru_cache

MODEL = os.getenv("EMBED_MODEL", "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")


@lru_cache(maxsize=1)
def _model():
    from fastembed import TextEmbedding

    return TextEmbedding(MODEL)


def embed(texts: list[str]) -> list[list[float]]:
    return [v.tolist() for v in _model().embed([t[:2000] for t in texts])]
