package com.leo.hekima.subs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.leo.hekima.subs.AlignmentFailure.*;
import static com.leo.hekima.subs.AlignmentSuccess.*;

public class SearchPattern {
    private static final Logger logger = LoggerFactory.getLogger(SearchPattern.class);
    public static final List<String> POS_TAG_NOT_WORD = newArrayList(
            "SC", "SE", "SF", "SSC", "SSO");
    public static final List<String> KEYWORDS = newArrayList(
            "<VSTEM>", "<ADJSTEM>", "≤NSTEM>", "<WORDS>", "<WORD>", "<VENDING>", "(", ")");
    public static final List<KwAndRpl> KEYWORDS_AND_REPLACEMENT = KEYWORDS.stream().map(SearchPattern::getKeywordAndReplacement).collect(Collectors.toList());;

    private static final Map<AlignmentSuccess, Float> SCORES = new HashMap<>();
    private static final Map<AlignmentFailure, Float> PENALTIES = new HashMap<>();
    static {
        final float fixWordScore = 1f;
        SCORES.put(FIX_WORD, fixWordScore);
        SCORES.put(NF_WORD, fixWordScore * 0.75f);
        SCORES.put(GOOD_VALUE, fixWordScore * 0.5f);
        SCORES.put(GOOD_TAG, fixWordScore * 0.5f);
        PENALTIES.put(MISSING_FIX_WORD, fixWordScore);
        PENALTIES.put(MISSING_NF_WORD, fixWordScore * 0.1f);
        PENALTIES.put(EXTRA_FIX_WORD, fixWordScore * 0.35f);
        PENALTIES.put(EXTRA_NF_WORD, fixWordScore * 0.1f);
        PENALTIES.put(WRONG_VALUE_GOOD_TAG, fixWordScore * 0.8f);
        PENALTIES.put(GOOD_VALUE_WRONG_TAG, fixWordScore * 0.5f);
    }

    public static List<PosTag> toSentence(final String raw, final Komoran posTagger) {
        final KomoranResult analysis = posTagger.analyze(raw);
        return analysis.getList().stream()
                .map(e -> new PosTag(e.getFirst(), e.getSecond()))
                .collect(Collectors.toList());
    }

    public static Multimap<PosTag, IndexEntry> index(final List<List<PosTag>> corpus) {
        final Multimap<PosTag, IndexEntry> db = HashMultimap.create();
        for (int i = 0; i < corpus.size(); i++) {
            final List<PosTag> sentence = corpus.get(i);
            for (int indexTag = 0; indexTag < sentence.size(); indexTag++) {
                final PosTag posTag = sentence.get(indexTag);
                db.put(posTag, new IndexEntry(i, indexTag));
            }
        }
        return db;
    }


    public static List<Integer> findFixMatches(final List<PosTag> analyzedQuery,
                                      final Multimap<PosTag, IndexEntry> index,
                                      final float minSimilarity) {
        final Map<Integer, Integer> countBySentenceInCorpus = new HashMap<>();
        for (PosTag posTag : analyzedQuery) {
            for (IndexEntry indexEntry : index.get(posTag)) {
                countBySentenceInCorpus.compute(indexEntry.sentenceIndex(), (k,v) -> v == null ? 1 : v + 1);
            }
        }
        final int minScore = (int)Math.floor(analyzedQuery.size() * minSimilarity);
        return countBySentenceInCorpus.entrySet().stream().filter(e -> e.getValue() >= minScore)
            .sorted((e1, e2) -> e2.getValue() - e1.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public static List<IndexWithScoreAndZone> scoreSentencesAgainstQuery(
            List<PosTag> analyzedQuery, List<Integer> candidates,
            List<List<PosTag>> corpus) {
        return candidates.stream().map(candidateIndex -> {
            final List<PosTag> candidate = corpus.get(candidateIndex);
            // 1. they should all be in the same order
            // 2. we lose points according to PENALTIES
            float score = 0;
            int lastPositionInCandidate = -1;
            int start = -1;
            for (PosTag queryLemme : analyzedQuery) {
                boolean found = false;
                for(int i = lastPositionInCandidate + 1; i < candidate.size() && !found; i ++) {
                    final PosTag analyzedLemme = candidate.get(i);
                    final Optional<AlignmentFailure> maybeFailure = getAlignementFailure(queryLemme, analyzedLemme);
                    if(maybeFailure.isPresent()) {
                        // For now we stop at the first possible value
                        AlignmentFailure failure = maybeFailure.get();
                        if(failure.equals(WRONG_VALUE_GOOD_TAG) || failure.equals(GOOD_VALUE_WRONG_TAG)) {
                            score -= PENALTIES.getOrDefault(maybeFailure.get(), 0f);
                            for(int j = lastPositionInCandidate + 1; j < i; j++) {
                                PosTag extra = candidate.get(j);
                                if(isFix(extra)) {
                                    score -= PENALTIES.get(EXTRA_FIX_WORD);
                                } else {
                                    score -= PENALTIES.get(EXTRA_NF_WORD);
                                }
                            }
                            found = true;
                            lastPositionInCandidate = i;
                            if(start == - 1) {
                                start = i;
                            }
                        }
                    } else {
                        found = true;
                        for(int j = lastPositionInCandidate + 1; j < i; j++) {
                            PosTag extra = candidate.get(j);
                            if(isFix(extra)) {
                                score -= PENALTIES.get(EXTRA_FIX_WORD);
                            } else {
                                score -= PENALTIES.get(EXTRA_NF_WORD);
                            }
                        }
                        lastPositionInCandidate = i;
                        if(start == - 1) {
                            start = i;
                        }
                    }
                }
                if(found) {
                    score +=  isFix(queryLemme) ? SCORES.get(FIX_WORD) : SCORES.get(NF_WORD);
                } else {
                    score -= PENALTIES.get(isFix(queryLemme) ? MISSING_FIX_WORD : MISSING_NF_WORD);
                }
            }
            return new IndexWithScoreAndZone(candidateIndex, score, start, lastPositionInCandidate);
        })
        .collect(Collectors.toList());
    }

    public static float getMaxScore(List<PosTag> analyzedQuery) {
        return (float)analyzedQuery.stream().mapToDouble(pt -> isFix(pt) ? SCORES.get(FIX_WORD) : SCORES.get(NF_WORD))
                .sum();
    }

    public static boolean isFix(PosTag extra) {
        final String type = extra.type();
        if(type.startsWith("S") || type.equals("EP") || type.equals("EC")) {
            return false;
        }
        return true;
    }

    private static Optional<AlignmentFailure> getAlignementFailure(PosTag queryLemme, PosTag analyzedLemme) {
        final Optional<AlignmentFailure> failure;
        if(queryLemme.value().equals(analyzedLemme.value())) {
            if(queryLemme.type().equals(analyzedLemme.type())) {
                failure = Optional.empty();
            } else {
                failure = Optional.of(GOOD_VALUE_WRONG_TAG);
            }
        } else {
            if(queryLemme.type().equals(analyzedLemme.type())) {
                failure = Optional.of(WRONG_VALUE_GOOD_TAG);
            } else {
                if(isFix(queryLemme)) {
                    failure = Optional.of(MISSING_FIX_WORD);
                } else {
                    failure = Optional.of(MISSING_NF_WORD);
                }
            }
        }
        return failure;
    }

    private Optional<String> getKeywordFromReplacement(String rpl) {
        return KEYWORDS_AND_REPLACEMENT.stream()
                .filter(kar -> kar.replacement.equals(rpl))
                .map(kar -> kar.keyword)
        .findAny();
    }

    private static final KwAndRpl getKeywordAndReplacement(final String keyword) {
        final String replacement;
        switch (keyword) {
            case "(":
                replacement = "STARTCONDITIONAL";
                break;
            case ")":
                replacement = "ENDCONDITIONAL";
                break;
            default:
                replacement = keyword.substring(1, keyword.length() - 1) + "MMMM";
        }
        return new KwAndRpl(keyword, replacement);
    }

    public static record KwAndRpl(String keyword, String replacement){}
}
