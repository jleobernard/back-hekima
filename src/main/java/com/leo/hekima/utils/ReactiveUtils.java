package com.leo.hekima.utils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class ReactiveUtils {
    public static <T> Mono<List<T>> orEmptyList(final Flux<T> flux) {
        return flux.collectList().switchIfEmpty(Mono.defer(() -> Mono.just(emptyList())));
    }
    public static <T> Mono<List<T>> emptyDeferred(final Class<T> clazz) {
        return Mono.defer(() -> Mono.just(new ArrayList<T>()));
    }
}
