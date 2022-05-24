package com.leo.hekima.subs;

import com.leo.hekima.to.SubsSearchRequest;

public record SubsSearchProblem(SubsSearchRequest request,
                                Sentence analyzedQuery,
                                float minScore, float maxScore) {
}
