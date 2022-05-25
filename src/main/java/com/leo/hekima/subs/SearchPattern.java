package com.leo.hekima.subs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.leo.hekima.to.NoAlternativeSearchRequest;
import com.leo.hekima.to.NoAlternativeSubsSearchPatternElement;
import com.leo.hekima.to.SubsSearchPatternElement;
import com.leo.hekima.to.SubsSearchRequest;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.core.model.LatticeNode;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import kr.co.shineware.nlp.komoran.parser.KoreanUnitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.leo.hekima.subs.AlignmentFailure.*;
import static com.leo.hekima.subs.AlignmentSuccess.*;
import static java.lang.System.arraycopy;
import static java.lang.System.in;

public class SearchPattern {
    private static final Logger logger = LoggerFactory.getLogger(SearchPattern.class);
    public static final List<String> KEYWORDS = newArrayList(
            "<VSTEM>", "<ADJSTEM>", "≤NSTEM>", "<WORDS>", "<WORD>", "<VENDING>", "(", ")");
    public static final List<KwAndRpl> KEYWORDS_AND_REPLACEMENT = KEYWORDS.stream().map(SearchPattern::getKeywordAndReplacement).collect(Collectors.toList());

    private static final Map<AlignmentSuccess, Float> SCORES = new HashMap<>();
    private static final Map<AlignmentFailure, Float> PENALTIES = new HashMap<>();
    private static final Field resultNodeListField = ReflectionUtils.findField(KomoranResult.class, "resultNodeList");
    static {
        final float fixWordScore = 1f;
        SCORES.put(FIX_WORD, fixWordScore);
        SCORES.put(NF_WORD, fixWordScore * 0.5f);
        SCORES.put(GOOD_FIX_VALUE, fixWordScore * 0.75f);
        SCORES.put(GOOD_NF_VALUE, fixWordScore * 0.25f);
        SCORES.put(GOOD_TAG, fixWordScore * 0.5f);
        PENALTIES.put(MISSING_FIX_WORD, fixWordScore);
        PENALTIES.put(MISSING_NF_WORD, fixWordScore * 0.1f);
        PENALTIES.put(EXTRA_FIX_WORD, fixWordScore * 0.35f);
        PENALTIES.put(EXTRA_NF_WORD, fixWordScore * 0.1f);
        PENALTIES.put(WRONG_FIX_VALUE_GOOD_TAG, fixWordScore * 0.8f);
        PENALTIES.put(GOOD_FIX_VALUE_WRONG_TAG, fixWordScore * 0.1f);
        PENALTIES.put(WRONG_NF_VALUE_GOOD_TAG, fixWordScore * 0.1f);
        PENALTIES.put(GOOD_NF_VALUE_WRONG_TAG, fixWordScore * 0.5f);
        ReflectionUtils.makeAccessible(resultNodeListField);
    }

    private static final KoreanUnitParser kp = new KoreanUnitParser();
    private static final Map<SearchableType, PatternBeacon> beacons = new HashMap<>();
    static {
        beacons.put(SearchableType.NOUN, new PatternBeacon("만화경", new String[]{"ㅁㅏㄴㅎㅘㄱㅕㅇ"}));
        beacons.put(SearchableType.VERB_STEM, new PatternBeacon("묶", new String[]{"ㅁㅜㄲ"}));
    }

    public static String jasoUnits(final String haystack) {
        return kp.parse(haystack);
    }

    private record Segment(int start, int end){}

    private record SegmentAndSearchElement(Segment segment, NoAlternativeSubsSearchPatternElement element){}

    private record SentenceElementAlternatives(int index, String[] alternatives) {}

    /**
     *
     * @param request
     * @param posTagger
     * @return All possible sentences
     */
    public static List<Sentence> toSentences(final SubsSearchRequest request, final Komoran posTagger) {
        final List<SentenceElementAlternatives> alternativesMarkers = new ArrayList<>();
        final SubsSearchPatternElement[] pattern = request.pattern();
        for (int i = 0; i < pattern.length; i++) {
            final SubsSearchPatternElement elt = pattern[i];
            if(elt.alternatives().isPresent() && elt.alternatives().get().length > 1) {
                alternativesMarkers.add(new SentenceElementAlternatives(i, elt.alternatives().get()));
            }
        }
        final List<NoAlternativeSearchRequest> sentences;
        final NoAlternativeSubsSearchPatternElement[] noAlternativeEltsTemplate =
            new NoAlternativeSubsSearchPatternElement[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            final var pelt = pattern[i];
            final var currentAlternatives = pelt.alternatives();
            if(currentAlternatives.isEmpty() || currentAlternatives.get().length <= 1) {
                var maybeValue = currentAlternatives.map(alts -> alts[0]);
                noAlternativeEltsTemplate[i] = new NoAlternativeSubsSearchPatternElement(maybeValue, pelt.posTag());
            }
        }
        if(alternativesMarkers.isEmpty()) {
            sentences = newArrayList(new NoAlternativeSearchRequest(noAlternativeEltsTemplate));
        } else {
            final List<AlternativeReplacement> allReplacement = replacementsCartesianProduct(alternativesMarkers);
            sentences = allReplacement.stream()
                .map(alternativeReplacement -> {
                    final NoAlternativeSubsSearchPatternElement[] oneAlernative = Arrays.copyOf(noAlternativeEltsTemplate,
                        noAlternativeEltsTemplate.length);
                    for (int i = 0; i < alternativeReplacement.indices().length; i++) {
                        oneAlernative[alternativeReplacement.indices()[i]] = alternativeReplacement.alternatives()[i];
                    }
                    return new NoAlternativeSearchRequest(oneAlernative);
                })
                .toList();
        }
        return sentences.stream().map(sentence -> toSentence(sentence, posTagger)).toList();

    }

    private static List<AlternativeReplacement> replacementsCartesianProduct(List<SentenceElementAlternatives> alternativesMarkers) {
        return replacementsCartesianProduct(alternativesMarkers, 0);
    }
    private static List<AlternativeReplacement> replacementsCartesianProduct(List<SentenceElementAlternatives> alternativesMarkers, final int index) {
        final List<AlternativeReplacement> replacements;
        if(index >= alternativesMarkers.size()) {
            replacements = Collections.emptyList();
        } else {
            replacements = new ArrayList<>();
            final SentenceElementAlternatives root = alternativesMarkers.get(index);
            final List<AlternativeReplacement> nextProduct = replacementsCartesianProduct(alternativesMarkers,
                index +1);
            final int eltIndex = root.index;
            final int nbElts = (alternativesMarkers.size() - index);
            for (String alternative : root.alternatives()) {
                if(nextProduct.isEmpty()) {
                    replacements.add(new AlternativeReplacement(
                        new int[]{eltIndex},
                        new NoAlternativeSubsSearchPatternElement[]{
                            new NoAlternativeSubsSearchPatternElement(Optional.ofNullable(alternative),
                            Optional.empty())}));
                } else {
                    for (AlternativeReplacement nextAlternativeReplacement : nextProduct) {
                        final int[] newIndices = new int[nbElts];
                        newIndices[0] = eltIndex;
                        arraycopy(nextAlternativeReplacement.indices(), 0, newIndices, 1, nbElts - 1);
                        final NoAlternativeSubsSearchPatternElement[] newAlternatives = new NoAlternativeSubsSearchPatternElement[nbElts];
                        newAlternatives[0] = new NoAlternativeSubsSearchPatternElement(Optional.ofNullable(alternative),
                            Optional.empty());
                        arraycopy(nextAlternativeReplacement.alternatives(), 0, newAlternatives, 1, nbElts - 1);
                        replacements.add(new AlternativeReplacement(newIndices, newAlternatives));
                    }
                }
            }
        }
        return replacements;
    }

    public static Sentence toSentence(final NoAlternativeSearchRequest request, final Komoran posTagger) {
        final StringBuilder sbuilder = new StringBuilder();
        final int patternLength = request.elements().length;
        int lastEnding = 0;
        final List<SegmentAndSearchElement> toReplace = new ArrayList<>();
        for (int i = 0; i < patternLength; i++) {
            final NoAlternativeSubsSearchPatternElement patternElement = request.elements()[i];
            final var value = patternElement.value();
            final String placeholder;
            final boolean segmentHasToBeReplaced;
            if(value.isEmpty()) {
                // No alternative, i.e. we are not seeking a particular word
                // but just a tag so we will replace this entry by a known, rare, entry
                final PatternBeacon beacon = beacons.get(patternElement.posTag().get());
                placeholder = beacon.placeholder();
                segmentHasToBeReplaced = true;
            } else {
                placeholder = value.get();
                segmentHasToBeReplaced = false;
            }
            sbuilder.append(placeholder);
            final int newEnding = lastEnding + jasoUnits(placeholder).length();
            if(segmentHasToBeReplaced) {
                toReplace.add(new SegmentAndSearchElement(new Segment(lastEnding, newEnding), patternElement));
            }
            lastEnding = newEnding;
        }
        final KomoranResult analysis = posTagger.analyze(sbuilder.toString());
        return replaceTokensThatNeedReplacing(analysis, toReplace);
    }

    private static Sentence replaceTokensThatNeedReplacing(final KomoranResult analysis,
                                                                             final List<SegmentAndSearchElement> toReplace) {
        final List<SentenceElement> result = new ArrayList<>();
        int i = 0;
        final List<LatticeNode> tokenList = (List<LatticeNode>) ReflectionUtils.getField(resultNodeListField, analysis);
        for (SegmentAndSearchElement eltToReplace : toReplace) {
            final int start = eltToReplace.segment.start;
            final int end = eltToReplace.segment.end;
            for (; i < tokenList.size(); i++) {
                final LatticeNode currentToken = tokenList.get(i);
                if (currentToken.getMorphTag().getMorph().equals("<end>")) {
                    continue;
                }
                if (currentToken.getBeginIdx() >= start) {
                    // If the current token is in the segment then we ignore it.....
                    if (currentToken.getEndIdx() >= end) {
                        // ... and if it is the last portion of the segment then we add the replacement
                        result.add(toSentenceElement(eltToReplace.element));
                        i++;
                        break;
                    }
                } else {
                    result.add(new SentenceElement(kp.combine(currentToken.getMorphTag().getMorph()),
                        currentToken.getTag()));
                }
            }
        }
        for(; i < tokenList.size(); i++) {
            final var currentToken = tokenList.get(i);
            if (currentToken.getMorphTag().getMorph().equals("<end>")) {
                continue;
            }
            result.add(new SentenceElement(kp.combine(currentToken.getMorphTag().getMorph()), currentToken.getTag()));
        }
        return new Sentence(result);
    }

    private static SentenceElement toSentenceElement(NoAlternativeSubsSearchPatternElement element) {
        return new SentenceElement(element.value(), element.posTag().map(SearchableType::getType));
    }

    public static Multimap<SentenceElement, IndexEntry> index(final List<List<SentenceElement>> corpus) {
        final Multimap<SentenceElement, IndexEntry> db = HashMultimap.create();
        for (int i = 0; i < corpus.size(); i++) {
            final List<SentenceElement> sentence = corpus.get(i);
            for (int indexTag = 0; indexTag < sentence.size(); indexTag++) {
                final SentenceElement sentenceElement = sentence.get(indexTag);
                db.put(sentenceElement, new IndexEntry(i, indexTag));
            }
        }
        return db;
    }


    /**
     * @param problem Problem related to the query to execute
     * @param index database in which to search for similarities
     * @return ids of the entries that are potential matches, i.e. ids of the entries that have sufficient
     * requested fixed values to be able to match the request in best case scenario.
     */
    public static List<Integer> findFixMatches(final SubsSearchProblem problem,
                                               final Multimap<SentenceElement, IndexEntry> index) {
        final List<SentenceElement> analyzedQuery = problem.analyzedQuery().elements();
        final Map<Integer, Integer> countBySentenceInCorpus = new HashMap<>();
        List<SentenceElement> fixParts = analyzedQuery.stream().filter(SearchPattern::isFix).toList();
        if(fixParts.isEmpty()){
            fixParts = analyzedQuery.stream().filter(elt -> elt.value().isPresent()).toList();
        }
        for (SentenceElement sentenceElement : fixParts) {
            for (IndexEntry indexEntry : index.get(sentenceElement)) {
                countBySentenceInCorpus.compute(indexEntry.sentenceIndex(), (k,v) -> v == null ? 1 : v + 1);
            }
        }
        final int minScore = (int)Math.ceil(fixParts.size() * problem.request().minSimilarity());
        return countBySentenceInCorpus.entrySet().stream().filter(e -> e.getValue() >= minScore)
            .sorted((e1, e2) -> e2.getValue() - e1.getValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private record SavePoint(int positionInQuery, int lastPositionInCandidate, float score, int start, boolean hasStarted){}

    public static List<IndexWithScoreAndZone> scoreSentencesAgainstQuery(
            Sentence analyzedQuery, List<Integer> candidates,
            List<List<SentenceElement>> corpus) {
        return candidates.stream().map(candidateIndex -> {
            final List<SentenceElement> candidate = corpus.get(candidateIndex);
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
                int i = lastPositionInCandidate + 1;
                for (; positionInQuery < analyzedQuery.elements().size(); positionInQuery++) {
                    SentenceElement queryLemme = analyzedQuery.elements().get(positionInQuery);
                    boolean found = false;
                    for (; i < candidate.size() && !found; i++) {
                        final SentenceElement analyzedLemme = candidate.get(i);
                        final Optional<AlignmentFailure> maybeFailure = getAlignementFailure(queryLemme, analyzedLemme);
                        if (maybeFailure.isPresent()) {
                            AlignmentFailure failure = maybeFailure.get();
                            // We will explore this possibility with a bad alignment and move on to the next lemme
                            // BUT we save the failure for next time if there is something else to analyse
                            if(positionInQuery < analyzedQuery.elements().size() - 1) {
                                savePoints.push(new SavePoint(positionInQuery + 1, lastPositionInCandidate, score - PENALTIES.get(isFix(queryLemme) ? MISSING_FIX_WORD : MISSING_NF_WORD), start, true));
                            }
                            if(!hasStarted) {
                                // If we haven't started yet to recognize the query we will skip to the next position
                                // with no penalty
                                savePoints.push(new SavePoint(0, i, 0, -1, false));
                            }
                            score -= PENALTIES.getOrDefault(failure, 0f);
                            switch (failure) {
                                case GOOD_FIX_VALUE_WRONG_TAG -> score += SCORES.getOrDefault(GOOD_FIX_VALUE, 0f);
                                case GOOD_NF_VALUE_WRONG_TAG -> score += SCORES.getOrDefault(GOOD_NF_VALUE, 0f);
                                case WRONG_FIX_VALUE_GOOD_TAG, WRONG_NF_VALUE_GOOD_TAG -> score += SCORES.getOrDefault(GOOD_TAG, 0f);
                            }
                            hasStarted = true;
                            for (int j = lastPositionInCandidate + 1; j < i; j++) {
                                SentenceElement extra = candidate.get(j);
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
                        } else {
                            found = true;
                            if(hasStarted) {
                                for (int j = lastPositionInCandidate + 1; j < i; j++) {
                                    SentenceElement extra = candidate.get(j);
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
                            score += isFix(queryLemme) ? SCORES.get(FIX_WORD) : SCORES.get(NF_WORD);
                        }
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

    public static float getMaxScore(final Sentence analyzedQuery) {
        return (float)analyzedQuery.elements().stream().mapToDouble(pt -> isFix(pt) ? SCORES.get(FIX_WORD) :
                SCORES.get(NF_WORD))
                .sum();
    }

    public static boolean isFix(SentenceElement extra) {
        return extra.value().isPresent() && extra.type().map(type -> {
            char firstLetter = type.charAt(0);
            return switch (firstLetter) {
                case 'S', 'E', 'J', 'X' -> false;
                default -> true;
            };
        }).orElse(true);
    }

    private static Optional<AlignmentFailure> getAlignementFailure(SentenceElement queryLemme, SentenceElement analyzedLemme) {
        final Optional<AlignmentFailure> failure;
        if(queryLemme.value().equals(analyzedLemme.value())) {
            if(sameType(queryLemme, analyzedLemme)) {
                failure = Optional.empty();
            } else {
                failure = Optional.of(isFix(queryLemme) ? GOOD_FIX_VALUE_WRONG_TAG : GOOD_NF_VALUE_WRONG_TAG);
            }
        } else {
            if(sameType(queryLemme, analyzedLemme)) {
                failure = Optional.of(isFix(queryLemme) ? WRONG_FIX_VALUE_GOOD_TAG : WRONG_NF_VALUE_GOOD_TAG);
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

    private static boolean sameType(SentenceElement queryLemme, SentenceElement analyzedLemme) {
        return
                queryLemme.type().orElse("°").charAt(0) ==
                analyzedLemme.type().orElse("§").charAt(0);
    }

    private Optional<String> getKeywordFromReplacement(String rpl) {
        return KEYWORDS_AND_REPLACEMENT.stream()
                .filter(kar -> kar.replacement.equals(rpl))
                .map(kar -> kar.keyword)
        .findAny();
    }

    private static KwAndRpl getKeywordAndReplacement(final String keyword) {
        final String replacement = switch (keyword) {
            case "(" -> "STARTCONDITIONAL";
            case ")" -> "ENDCONDITIONAL";
            default -> keyword.substring(1, keyword.length() - 1) + "MMMM";
        };
        return new KwAndRpl(keyword, replacement);
    }

    public record KwAndRpl(String keyword, String replacement){}
}
