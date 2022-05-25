package com.leo.hekima;

import com.leo.hekima.subs.SearchableType;
import com.leo.hekima.subs.SubsSearchProblem;
import com.leo.hekima.to.SubsSearchRequest;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.leo.hekima.subs.SearchPattern.*;
import static com.leo.hekima.subs.SubsService.createDbFromSentences;
import static com.leo.hekima.subs.SubsService.parseQueryElements;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubsServiceTest {

    private static final Komoran tagger = new Komoran(DEFAULT_MODEL.FULL);

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

    @Test
    public void testFindFixMatchesNominal() {
        final var db = createDbFromSentences(
            "학생들은 바보",
            "우리는 예술가입니다",
            "나는 TV를보고 있어요",
            "저는 학생입니다"
        );
        SubsSearchProblem problem = toProblem("학생+입니다", 0.9f);
        List<Integer> matches = findFixMatches(problem, db);
        assertEquals(1, matches.size());
        assertEquals(3, matches.get(0));

        problem = toProblem("학생+입니다", 0.5f);
        matches = findFixMatches(problem, db);
        assertTrue(matches.contains(3));
        assertTrue(matches.contains(0));
        assertTrue(matches.contains(1));
    }

    @Test
    public void testFindFixMatchesWithPlaceHolders() {
        final var db = createDbFromSentences(
            "학생들은 바보",
            "우리는 예술가입니다",
            "나는 TV를보고 있어요",
            "저는 학생입니다"
        );
        SubsSearchProblem problem = toProblem(":N+입니다", 0.5f);
        List<Integer> matches = findFixMatches(problem, db);
        assertEquals(2, matches.size());
        assertTrue(matches.contains(3));
        assertTrue(matches.contains(1));
    }

    public static SubsSearchProblem toProblem(final String q) {
        return toProblem(q, 0.75f);
    }
    public static SubsSearchProblem toProblem(final String q, final float miniSimilarity) {
        final var elts = parseQueryElements(q);
        final var query = new SubsSearchRequest(q, false, elts, miniSimilarity, 1f, false);
        final var sentence = toSentences(query, tagger).get(0);
        return new SubsSearchProblem(
            query,
            sentence,
            getMaxScore(sentence) * query.minSimilarity(),
            getMaxScore(sentence) * query.maxSimilarity()
        );
    }

}
