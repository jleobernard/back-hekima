package com.leo.hekima;

import com.leo.hekima.subs.SearchableType;
import org.junit.jupiter.api.Test;

import static com.leo.hekima.subs.SubsService.parseQueryElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubsServiceTest {
    @Test
    public void testParseEmptyQuery() {
        assertEquals(0, parseQueryElements("").length);
        assertEquals(0, parseQueryElements("      ").length);
        assertEquals(0, parseQueryElements(null).length);
    }
    @Test
    public void testParseQueryWithOneElementAndNoPosTag() {
        var parsed = parseQueryElements("나");
        assertEquals(1,parsed.length);
        assertEquals(1, parsed[0].alternatives().get().length);
        assertEquals("나", parsed[0].alternatives().get()[0]);
        assertTrue(parsed[0].posTag().isEmpty());
    }
    @Test
    public void testParseQueryWithOneElementAndPosTag() {
        var parsed = parseQueryElements("나:V");
        assertEquals(1,parsed.length);
        assertEquals(1, parsed[0].alternatives().get().length);
        assertEquals("나", parsed[0].alternatives().get()[0]);
        assertEquals(SearchableType.VERB_STEM, parsed[0].posTag().get());
    }
    @Test
    public void testParseQueryWithOneElementAndOnlyPosTag() {
        var parsed = parseQueryElements(":N");
        assertEquals(1,parsed.length);
        assertTrue(parsed[0].alternatives().isEmpty());
        assertEquals(SearchableType.NOUN, parsed[0].posTag().get());
    }
    @Test
    public void testParseQueryMixed() {
        var parsed = parseQueryElements("나:V+는+한국/프랑스:N+이다");
        assertEquals(4,parsed.length);
        int i = 0;
        assertEquals(1, parsed[i].alternatives().get().length);
        assertEquals("나", parsed[i].alternatives().get()[0]);
        assertEquals(SearchableType.VERB_STEM, parsed[i].posTag().get());

        i++;
        assertEquals(1, parsed[i].alternatives().get().length);
        assertEquals("는", parsed[i].alternatives().get()[0]);
        assertTrue(parsed[i].posTag().isEmpty());

        i++;
        assertEquals(2, parsed[i].alternatives().get().length);
        assertEquals("한국", parsed[i].alternatives().get()[0]);
        assertEquals("프랑스", parsed[i].alternatives().get()[1]);
        assertEquals(SearchableType.NOUN, parsed[i].posTag().get());

        i++;
        assertEquals(1, parsed[i].alternatives().get().length);
        assertEquals("이다", parsed[i].alternatives().get()[0]);
        assertTrue(parsed[i].posTag().isEmpty());
    }

}
