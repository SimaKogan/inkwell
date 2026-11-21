package com.inkwell.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LanguageDetectorTest {
    @Test void detectsScripts() {
        assertEquals("he", LanguageDetector.code("שלום עולם"));
        assertEquals("ru", LanguageDetector.code("привет, мир"));
        assertEquals("en", LanguageDetector.code("hello world"));
        assertEquals("mixed", LanguageDetector.code("hello привет"));
        assertEquals("unknown", LanguageDetector.code("12345 !?"));
    }
}
