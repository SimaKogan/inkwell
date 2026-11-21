package com.inkwell.search;

import java.util.HashMap;
import java.util.Map;

/**
 * Keyboard-layout repair: text typed with the wrong layout active (QWERTY ⇄ Hebrew SI-1452, QWERTY ⇄ Russian ЙЦУКЕН).
 * Ported from the JibToHeb MCP server and extended with Russian.
 */
public final class Layouts {
    private Layouts() {}

    private static final String QWERTY = "qwertyuiopasdfghjklzxcvbnm;',./";
    private static final String HEBREW = "/'קראטוןםפשדגכעיחלךזסבהנמצף,תץ.";

    private static final String QWERTY_RU = "qwertyuiop[]asdfghjkl;'zxcvbnm,.`";
    private static final String RUSSIAN = "йцукенгшщзхъфывапролджэячсмитьбюё";
    private static final String SHIFT_RU_FROM = "{}:\"<>~";
    private static final String SHIFT_RU_TO = "ХЪЖЭБЮЁ";

    private static final Map<Character, Character> EN_HE = zip(QWERTY, HEBREW);
    private static final Map<Character, Character> HE_EN = invert(EN_HE);
    private static final Map<Character, Character> EN_RU = zip(QWERTY_RU, RUSSIAN);
    private static final Map<Character, Character> RU_EN = invert(EN_RU);

    /** "akuo" → "שלום" */
    public static String toHebrew(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(EN_HE.getOrDefault(Character.toLowerCase(c), c));
        return sb.toString();
    }

    /** "ghbdtn" → "привет" */
    public static String toRussian(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            int shifted = SHIFT_RU_FROM.indexOf(c);
            if (shifted >= 0) {
                sb.append(SHIFT_RU_TO.charAt(shifted));
                continue;
            }
            Character mapped = EN_RU.get(Character.toLowerCase(c));
            if (mapped == null) sb.append(c);
            else sb.append(Character.isUpperCase(c) ? Character.toUpperCase(mapped) : mapped);
        }
        return sb.toString();
    }

    /** "שלום" typed while meaning Latin keys → "akuo" */
    public static String fromHebrew(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) sb.append(HE_EN.getOrDefault(c, c));
        return sb.toString();
    }

    /** "руддщ" → "hello" */
    public static String fromRussian(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            Character mapped = RU_EN.get(Character.toLowerCase(c));
            if (mapped == null) sb.append(c);
            else sb.append(Character.isUpperCase(c) ? Character.toUpperCase(mapped) : mapped);
        }
        return sb.toString();
    }

    private static Map<Character, Character> zip(String from, String to) {
        if (from.length() != to.length()) throw new IllegalStateException("layout tables differ in length");
        Map<Character, Character> m = new HashMap<>();
        for (int i = 0; i < from.length(); i++) m.put(from.charAt(i), to.charAt(i));
        return Map.copyOf(m);
    }

    private static Map<Character, Character> invert(Map<Character, Character> m) {
        Map<Character, Character> out = new HashMap<>();
        m.forEach((k, v) -> out.put(v, k));
        return Map.copyOf(out);
    }
}
