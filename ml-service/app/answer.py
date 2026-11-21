import os

SYSTEM = (
    "You answer questions about a catalog and its community posts. Use ONLY the numbered sources. "
    "Cite sources inline like [1]. Reply in the language of the question, in at most 5 sentences. "
    "If the sources do not contain the answer, say so plainly."
)


def answer(query: str, sources: list[dict]) -> str:
    import anthropic

    context = "\n\n".join(f"[{s['n']}] {s['title']}\n{s['text']}" for s in sources)
    client = anthropic.Anthropic()
    msg = client.messages.create(
        model=os.getenv("ANTHROPIC_MODEL", "claude-sonnet-5-5"),
        max_tokens=600,
        system=SYSTEM,
        messages=[{"role": "user", "content": f"Sources:\n{context}\n\nQuestion: {query}"}],
    )
    return "".join(b.text for b in msg.content if b.type == "text").strip()
