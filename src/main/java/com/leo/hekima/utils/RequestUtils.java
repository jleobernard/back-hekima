package com.leo.hekima.utils;

import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class RequestUtils {
    private static final Integer MAX_COUNT = 100;

    public static final int getCount(final ServerRequest request) {
        return request.queryParam("count")
            .map(Integer::parseInt)
            .map(count -> count < 0 ? 1 : count)
            .map(count -> count > MAX_COUNT ? MAX_COUNT : count)
            .orElse(20);
    }

    public static final int getOffset(final ServerRequest request) {
        return request.queryParam("offset")
                .map(Integer::parseInt).filter(count -> count >= 0).orElse(0);
    }


    public static Set<String> getStringSet(ServerRequest serverRequest, String name) {
        return serverRequest.queryParam(name)
                .map(raw -> Arrays.stream(raw.split(","))
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toSet()))
                .orElseGet(HashSet::new);
    }
}
