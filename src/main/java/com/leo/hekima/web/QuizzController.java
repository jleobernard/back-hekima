package com.leo.hekima.web;

import com.leo.hekima.service.QuizzService;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.ElementSummaryView;
import com.leo.hekima.to.QuizzAnswerRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/api")
public class QuizzController {
    private final QuizzService quizzService;

    public QuizzController(QuizzService quizzService) {
        this.quizzService = quizzService;
    }
    @GetMapping("/quizz:generate")
    public Flux<ElementSummaryView> generate(
        @RequestParam(name = "tags", required = false) final Set<String> tags,
        @RequestParam(name = "notTags", required = false) final Set<String> notTags,
        @RequestParam(name = "sources", required = false) final Set<String> sources,
        @RequestParam(name = "count", required = false, defaultValue = "20") final Integer count,
        @RequestParam(name = "offset", required = false, defaultValue = "0") final Integer offset) {
        return quizzService.generate(tags, notTags, sources, count, offset);
    }
    @PostMapping("/quizz:answer")
    public Mono<AckResponse> answer(@RequestBody final QuizzAnswerRequest request) {
        return quizzService.answer(request);
    }
}
