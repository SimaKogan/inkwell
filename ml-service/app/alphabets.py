"""Character sets and the fixed layout of the handwriting sheets."""

RU_LOWER = "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
RU_UPPER = RU_LOWER.upper()
EN_LOWER = "abcdefghijklmnopqrstuvwxyz"
EN_UPPER = EN_LOWER.upper()
HE = "אבגדהוזחטיכךלמםנןסעפףצץקרשת"  # 22 letters + 5 final forms
DIGITS = "0123456789"
PUNCT = ".,!?:;-—()\"'«»"

# Digits and punctuation are on every sheet, so each extra sheet adds style variants.
SHEETS: dict[int, dict] = {
    0: {"key": "ru", "title": "Russian", "chars": RU_LOWER + RU_UPPER + DIGITS + PUNCT},
    1: {"key": "en", "title": "English", "chars": EN_LOWER + EN_UPPER + DIGITS + PUNCT},
    2: {"key": "he", "title": "Hebrew", "chars": HE + DIGITS + PUNCT},
}
KEY_TO_ID = {s["key"]: i for i, s in SHEETS.items()}

HEBREW_RANGE = (0x0590, 0x05FF)


def is_hebrew(ch: str) -> bool:
    return HEBREW_RANGE[0] <= ord(ch) <= HEBREW_RANGE[1]


def detect_language(text: str) -> str:
    """Coarse script-based language guess: he / ru / en / mixed / unknown."""
    he = ru = en = 0
    for ch in text:
        if not ch.isalpha():
            continue
        if is_hebrew(ch):
            he += 1
        elif "\u0400" <= ch <= "\u04ff":
            ru += 1
        elif ch.isascii():
            en += 1
    total = he + ru + en
    if total == 0:
        return "unknown"
    best = max((he, "he"), (ru, "ru"), (en, "en"))
    return best[1] if best[0] / total >= 0.8 else "mixed"
