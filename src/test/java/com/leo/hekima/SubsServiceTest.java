package com.leo.hekima;

import org.junit.jupiter.api.Test;

import static com.leo.hekima.subs.SubsService.parseQueryElements;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(1, parsed[0].alternatives().length);
        assertEquals("나", parsed[0].alternatives()[0]);
        assertEquals("", parsed[0].posTag());
    }
    @Test
    public void testParseQueryWithOneElementAndPosTag() {
        var parsed = parseQueryElements("나:V");
        assertEquals(1,parsed.length);
        assertEquals(1, parsed[0].alternatives().length);
        assertEquals("나", parsed[0].alternatives()[0]);
        assertEquals("V", parsed[0].posTag());
    }
    @Test
    public void testParseQueryMixed() {
        var parsed = parseQueryElements("나:V+는+한국/프랑스:N+이다");
        assertEquals(4,parsed.length);
        int i = 0;
        assertEquals(1, parsed[i].alternatives().length);
        assertEquals("나", parsed[i].alternatives()[0]);
        assertEquals("V", parsed[i].posTag());

        i++;
        assertEquals(1, parsed[i].alternatives().length);
        assertEquals("는", parsed[i].alternatives()[0]);
        assertEquals("", parsed[i].posTag());

        i++;
        assertEquals(2, parsed[i].alternatives().length);
        assertEquals("한국", parsed[i].alternatives()[0]);
        assertEquals("프랑스", parsed[i].alternatives()[1]);
        assertEquals("N", parsed[i].posTag());

        i++;
        assertEquals(1, parsed[i].alternatives().length);
        assertEquals("이다", parsed[i].alternatives()[0]);
        assertEquals("", parsed[i].posTag());
    }
}
