package com.leo.hekima.subs;

import java.util.ArrayList;
import java.util.List;

public class SearchState {
    private final int index;
    private final boolean start;
    private final List<SearchStateTransition> transitions;

    public SearchState(int index) {
        this(index, false);
    }
    public SearchState(int index, boolean start) {
        this.index = index;
        this.start = start;
        this.transitions = new ArrayList<>();
    }

    public SearchState addTransition(final SearchState newState,
                                     final String content,
                                     final String tag) {
        transitions.add(new SearchStateTransition(newState, content, tag));
        return this;
    }

    public boolean isFinal() {
        return transitions.isEmpty();
    }

    public List<SearchStateTransition> getTransitions() {
        return transitions;
    }

    public int getIndex() {
        return index;
    }
}
