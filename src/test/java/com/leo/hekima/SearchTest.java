package com.leo.hekima;


import com.google.common.collect.Multimap;
import com.leo.hekima.subs.IndexEntry;
import com.leo.hekima.subs.IndexWithScoreAndZone;
import com.leo.hekima.subs.PosTag;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.leo.hekima.subs.SearchPattern.*;

public class SearchTest {

    @Test
    public void testComplexSearch() {
        final List<String> haystack = newArrayList(
                "잘들 지내셨죠?",
                "잘 지냈어요"
        );
        final String q = "잘 지내셨어요";
        final Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        final List<PosTag> analyzedQuery = toSentence(q, komoran);
        final List<List<PosTag>> corpus = haystack.stream().map(h -> toSentence(h, komoran)).collect(Collectors.toList());
        System.out.println("Analysis for the query :");
        System.out.println(analyzedQuery);
        System.out.println("Analysis for the haystack :");
        System.out.println(corpus.get(1));
        final Multimap<PosTag, IndexEntry> index = index(corpus);
        final List<Integer> firstCandidates = findFixMatches(analyzedQuery, index, 0.5f);
        System.out.println("Candidates are :");
        System.out.println(firstCandidates);
        final List<IndexWithScoreAndZone> matches = scoreSentencesAgainstQuery(analyzedQuery, firstCandidates, corpus);
        System.out.println("Matches are : ");
        System.out.println(matches);
    }
}
