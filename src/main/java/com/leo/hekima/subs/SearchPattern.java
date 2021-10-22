package com.leo.hekima.subs;

import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.util.ArrayStack;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class SearchPattern {
    private static final Logger logger = LoggerFactory.getLogger(SearchPattern.class);
    public static final List<String> POS_TAG_NOT_WORD = newArrayList(
            "SC", "SE", "SF", "SSC", "SSO");
    public static final List<String> KEYWORDS = newArrayList(
            "<VSTEM>", "<ADJSTEM>", "≤NSTEM>", "<WORDS>", "<WORD>", "<VENDING>", "(", ")");
    public static final List<KwAndRpl> KEYWORDS_AND_REPLACEMENT = KEYWORDS.stream().map(SearchPattern::getKeywordAndReplacement).collect(Collectors.toList());;

    private final List<PosTag> fixWords;
    private final SearchState stateMachine;

    public SearchPattern(final String query, final Komoran posTagger) {
        String pattern = query;
        for (KwAndRpl kwAndRpl : KEYWORDS_AND_REPLACEMENT) {
            pattern = pattern.replace(kwAndRpl.keyword, kwAndRpl.replacement);
        }
        final KomoranResult analysis = posTagger.analyze(pattern);
        final List<PosTag> queryTags = analysis.getList().stream()
                .map(e -> new PosTag(e.getFirst(), e.getSecond()))
                .collect(Collectors.toList());
        //TODO trouver une parade pour 이다
        for (int i = 0; i < queryTags.size() - 1; i++) {
            final PosTag qt = queryTags.get(i);
            if(qt.type().equals("V") && queryTags.get(i + 1).equals("다")) {
                queryTags.set(i + 1, new PosTag(
                    getKeywordAndReplacement("<VENDING>").keyword, "SL"));
            }
        }
        fixWords = queryTags.stream().filter(qt -> !qt.type().equals("S")).collect(Collectors.toList());
        final SearchState start = new SearchState(-1, true);
        final ArrayStack<List<SearchState>> forkStates = new ArrayStack<>(10);
        final List<SearchState> statesToLink = newArrayList(start);
        int index = 0;
        for (PosTag queryTag : queryTags) {
            if(queryTag.type().equals("SL")) {
                final String keyword = getKeywordFromReplacement(queryTag.value()).orElse("");
                String pos_tag_to_match = null;
                switch (keyword) {
                    case "<VSTEM>": pos_tag_to_match = "VSTEM"; break;
                    case "<ADJSTEM>": pos_tag_to_match = "ADJSTEM"; break;
                    case "<NSTEM>": pos_tag_to_match = "NSTEM"; break;
                    case "<VENDING>":
                        for (SearchState searchState : statesToLink) {
                            searchState.addTransition(searchState, null, "VENDING");
                        }
                        break;
                    case "<WORDS>":
                        for (SearchState searchState : statesToLink) {
                            searchState.addTransition(searchState, null, "WORDS");
                        }
                        break;
                    case "(":
                        forkStates.push(statesToLink);
                        break;
                    case ")":
                        statesToLink.addAll(forkStates.pop());
                        break;
                }
                if(pos_tag_to_match != null) {
                    var newState = new SearchState(index);
                    index++;
                    for (SearchState searchState : statesToLink) {
                        searchState.addTransition(searchState, null, pos_tag_to_match);
                    }
                    statesToLink.clear();
                    statesToLink.add(newState);
                }
            } else {
                var newState = new SearchState(index);
                index++;
                for (SearchState stateToLink : statesToLink) {
                    stateToLink.addTransition(newState, queryTag.value(), queryTag.value());
                }
                statesToLink.clear();
                statesToLink.add(newState);
            }
        }
        stateMachine = start;
    }

    public boolean matches(final List<PosTag> taggedSentences) {
        for (int i = 0; i < taggedSentences.size(); i++) {
            if(matches(taggedSentences, i)); {
                return true;
            }
        }
        return false;
    }
    public boolean matches(final List<PosTag> taggedSentences, final int start) {
        var currStates = newArrayList(this.stateMachine);
        for(int i = start; i < taggedSentences.size(); i++) {
            final var lemme = taggedSentences.get(i);
            final Map<Integer, SearchState> newStates = new HashMap<>();
            for (SearchState currState : currStates) {
                for (SearchStateTransition transition : currState.getTransitions()) {
                    final var newState = transition.newState();
                    if(!newStates.containsKey(newState.getIndex()) && transition.canLemmeTransit(lemme)) {
                        if(newState.isFinal()) {
                            return true;
                        }
                        newStates.put(newState.getIndex(), newState);
                    }
                }
            }
            currStates = new ArrayList<>(newStates.values());
            if(currStates.isEmpty()) {
                return false;
            }
        }
        return false;
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

    public List<PosTag> getFixWords() {
        return this.fixWords;
    }


    public static record KwAndRpl(String keyword, String replacement){}
}
