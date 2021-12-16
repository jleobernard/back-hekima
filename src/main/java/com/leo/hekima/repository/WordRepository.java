package com.leo.hekima.repository;


import com.leo.hekima.model.WordModel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface WordRepository extends ReactiveCrudRepository<WordModel, Long> {
    Mono<WordModel> findByWord(String word);

    Flux<WordModel> findByWordIn(Set<String> indexableWords);

    @Query("select word FROM word WHERE word like :q order by length(word) asc")
    Flux<String> findByWordLikeOrderedByLengthAsc(@Param("q") String q);
}
