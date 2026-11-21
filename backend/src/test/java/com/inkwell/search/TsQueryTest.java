package com.inkwell.search;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TsQueryTest {
    @Test void buildsPrefixQuery() {
        assertEquals("мастер:* & маргарита:*", SearchService.tsQuery("Мастер Маргарита"));
    }

    @Test void stripsQuerySyntax() {
        assertEquals("a:* & b:*", SearchService.tsQuery("a' | & ! ( b"));
        assertEquals("", SearchService.tsQuery("&|!()'"));
    }

    @Test void capsTokenCount() {
        assertEquals(8, SearchService.tsQuery("a b c d e f g h i j k").split(" & ").length);
    }
}
