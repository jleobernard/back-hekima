package com.leo.hekima;


import com.fasterxml.jackson.core.type.TypeReference;
import com.leo.hekima.subs.SubsEntryView;
import com.leo.hekima.subs.SubsService;
import com.leo.hekima.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

public class SearchTest {

    final SubsService subsService = SubsService.fromMemory("저는 학생입니다",
        "무서워?",
        "당신은 집에 갈 수 있습니다",
        "당신은 배고프다",
        "그는 여기서 일한다",
        "나는 포도를 먹을까요");

    @Test
    public void testSearchFixVerb() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri("/search?q=있다")
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 1 result" , 1, results.size());
                assertEquals("Sub found is not good", "당신은 집에 갈 수 있습니다", results.get(0).subs());
            });
    }

    @Test
    public void testSearchFixNoun() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri("/search?q=집")
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 1 result" , 1, results.size());
                assertEquals("Sub found is not good", "당신은 집에 갈 수 있습니다", results.get(0).subs());
            });
    }

    @Test
    public void testSearchAlternative() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri("/search?q=집/학생")
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 2 results" , 2, results.size());
                final Set<String> subs = results.stream().map(SubsEntryView::subs).collect(Collectors.toSet());
                assertTrue("Should have found this one", subs.contains("당신은 집에 갈 수 있습니다"));
                assertTrue("Should have found this one", subs.contains("저는 학생입니다"));
            });
    }

    @Test
    public void testSearchVerbstemWithEnding() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri(uriBuilder ->  uriBuilder.path("/search").queryParam("q","{pattern}").build(":V+습니다"))
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 1 results" , 1, results.size());
                final Set<String> subs = results.stream().map(SubsEntryView::subs).collect(Collectors.toSet());
                assertTrue("Should have found this one", subs.contains("당신은 집에 갈 수 있습니다"));
            });
    }

    @Test
    public void testSearchComplex() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri(uriBuilder ->  uriBuilder.path("/search").queryParam("q","{pattern}").build(":V" +
                "+ㄹ/을/를+까요"))
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 1 results" , 1, results.size());
                final Set<String> subs = results.stream().map(SubsEntryView::subs).collect(Collectors.toSet());
                assertTrue("Should have found this one", subs.contains("나는 포도를 먹을까요"));
            });
    }

    @Test
    public void testSearchExact() {
        RouterFunction function = RouterFunctions.route(
            RequestPredicates.GET("/search"),
            subsService::search
        );

        WebTestClient
            .bindToRouterFunction(function)
            .build().get().uri(uriBuilder ->  uriBuilder.path("/search")
                .queryParam("q","{pattern}")
                .queryParam("exact", "true")
                .build("당신은 집"))
            .exchange()
            .expectStatus().isOk()
            .expectBody().consumeWith(data -> {
                final var results = JsonUtils.deserializeSilentFail(new String(data.getResponseBody()),
                    new TypeReference<List<SubsEntryView>>(){});
                assertEquals("Should have found 1 results" , 1, results.size());
                final Set<String> subs = results.stream().map(SubsEntryView::subs).collect(Collectors.toSet());
                assertTrue("Should have found this one", subs.contains("당신은 집에 갈 수 있습니다"));
            });
    }
}
