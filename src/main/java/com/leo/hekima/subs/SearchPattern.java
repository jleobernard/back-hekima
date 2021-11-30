package com.leo.hekima.subs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
        List<PosTag> fixParts = analyzedQuery.stream().filter(SearchPattern::isFix).collect(Collectors.toList());
        if(fixParts.isEmpty()){
            fixParts = analyzedQuery;
        }
        for (PosTag posTag : fixParts) {
            for (IndexEntry indexEntry : index.get(posTag)) {
                countBySentenceInCorpus.compute(indexEntry.sentenceIndex(), (k,v) -> v == null ? 1 : v + 1);
            }
        }
        final int minScore = (int)Math.floor(fixParts.size() * minSimilarity);
        return countBySentenceInCorpus.entrySet().stream().filter(e -> e.getValue() >= minScore)
            .sorted((e1, e2) -> e2.getValue() - e1.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private record SavePoint(int positionInQuery, int lastPositionInCandidate, float score, int start, boolean hasStarted){}

    public static List<IndexWithScoreAndZone> scoreSentencesAgainstQuery(
            List<PosTag> analyzedQuery, List<Integer> candidates,
            List<List<PosTag>> corpus) {
        return candidates.stream().map(candidateIndex -> {
            final List<PosTag> candidate = corpus.get(candidateIndex);
            // 1. they should all be in the same order
            // 2. we lose points according to PENALTIES
            final Stack<SavePoint> savePoints = new Stack<>();
            SavePoint bestScore = new SavePoint(-1, -1, Integer.MIN_VALUE, -1, false);
            savePoints.push(new SavePoint(0, -1, 0f, -1, false));
            while(!savePoints.empty()) {
                final SavePoint savePoint = savePoints.pop();
                float score = savePoint.score;
                int lastPositionInCandidate = savePoint.lastPositionInCandidate;
                int start = savePoint.start;
                int positionInQuery = savePoint.positionInQuery;
                boolean hasStarted = savePoint.hasStarted;
                for (int i = lastPositionInCandidate + 1; positionInQuery < analyzedQuery.size(); positionInQuery++) {
                    PosTag queryLemme = analyzedQuery.get(positionInQuery);
                    boolean found = false;
                    for (; i < candidate.size() && !found; i++) {
                        final PosTag analyzedLemme = candidate.get(i);
                        final Optional<AlignmentFailure> maybeFailure = getAlignementFailure(queryLemme, analyzedLemme);
                        if (maybeFailure.isPresent()) {
                            // We will explore this possibility with a bad alignment and move on to the next lemme
                            // BUT we save the failure for next time if there is something else to analyse
                            if(positionInQuery < analyzedQuery.size() - 1) {
                                savePoints.push(new SavePoint(positionInQuery + 1, lastPositionInCandidate, score - PENALTIES.get(isFix(queryLemme) ? MISSING_FIX_WORD : MISSING_NF_WORD), start, true));
                                if(!hasStarted) {
                                    savePoints.push(new SavePoint(positionInQuery + 1, positionInQuery, 0, start, false));
                                }
                            }
                            hasStarted = false;
                            AlignmentFailure failure = maybeFailure.get();
                            if (failure.equals(WRONG_VALUE_GOOD_TAG) || failure.equals(GOOD_VALUE_WRONG_TAG)) {
                                score -= PENALTIES.getOrDefault(maybeFailure.get(), 0f);
                                for (int j = lastPositionInCandidate + 1; j < i; j++) {
                                    PosTag extra = candidate.get(j);
                                    if (isFix(extra)) {
                                        score -= PENALTIES.get(EXTRA_FIX_WORD);
                                    } else {
                                        score -= PENALTIES.get(EXTRA_NF_WORD);
                                    }
                                }
                                found = true;
                                lastPositionInCandidate = i;
                                if (start == -1) {
                                    start = i;
                                }
                            }
                        } else {
                            found = true;
                            if(hasStarted) {
                                for (int j = lastPositionInCandidate + 1; j < i; j++) {
                                    PosTag extra = candidate.get(j);
                                    if (isFix(extra)) {
                                        score -= PENALTIES.get(EXTRA_FIX_WORD);
                                    } else {
                                        score -= PENALTIES.get(EXTRA_NF_WORD);
                                    }
                                }
                            }
                            hasStarted = true;
                            lastPositionInCandidate = i;
                            if (start == -1) {
                                start = i;
                            }
                        }
                    }
                    if (found) {
                        score += isFix(queryLemme) ? SCORES.get(FIX_WORD) : SCORES.get(NF_WORD);
                    } else {
                        score -= PENALTIES.get(isFix(queryLemme) ? MISSING_FIX_WORD : MISSING_NF_WORD);
                    }
                }
                if(score > bestScore.score) {
                    bestScore = new SavePoint(-1, lastPositionInCandidate, score, start, true);
                }
            }
            return new IndexWithScoreAndZone(candidateIndex, bestScore.score, bestScore.start, bestScore.lastPositionInCandidate);
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
