package com.inkwell.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LayoutsTest {
    @Test void latinTypedOnHebrewKeysBecomesHebrew() {
        assertEquals("שלום", Layouts.toHebrew("akuo"));
        assertEquals("שלום", Layouts.toHebrew("AKUO"));
    }

    @Test void hebrewRoundTrips() {
        assertEquals("akuo", Layouts.fromHebrew("שלום"));
    }

    @Test void latinTypedOnRussianKeysBecomesRussian() {
        assertEquals("привет", Layouts.toRussian("ghbdtn"));
        assertEquals("Привет", Layouts.toRussian("Ghbdtn"));
        assertEquals("хъжэбюё", Layouts.toRussian("[];',.`"));
    }

    @Test void russianRoundTrips() {
        assertEquals("hello", Layouts.fromRussian("руддщ"));
        assertEquals("Hello", Layouts.fromRussian("Руддщ"));
    }

    @Test void unmappedCharactersArePreserved() {
        assertEquals("שלום 123", Layouts.toHebrew("akuo 123"));
    }
}
