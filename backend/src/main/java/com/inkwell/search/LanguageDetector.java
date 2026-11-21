package com.inkwell.search;

/** Script-based language detection (enough to pick the right layout repair and to label results). */
public final class LanguageDetector {
    private LanguageDetector() {}

    public enum Script { LATIN, CYRILLIC, HEBREW, MIXED, NONE }

    public static Script script(String text) {
        int latin = 0, cyr = 0, heb = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetter(cp)) continue;
            switch (Character.UnicodeScript.of(cp)) {
                case LATIN -> latin++;
                case CYRILLIC -> cyr++;
                case HEBREW -> heb++;
                default -> { }
            }
        }
        int total = latin + cyr + heb;
        if (total == 0) return Script.NONE;
        if (latin == total) return Script.LATIN;
        if (cyr == total) return Script.CYRILLIC;
        if (heb == total) return Script.HEBREW;
        return Script.MIXED;
    }

    /** ISO-like code used by the UI: en, ru, he, mixed or unknown. */
    public static String code(String text) {
        return switch (script(text)) {
            case LATIN -> "en";
            case CYRILLIC -> "ru";
            case HEBREW -> "he";
            case MIXED -> "mixed";
            case NONE -> "unknown";
        };
    }
}
