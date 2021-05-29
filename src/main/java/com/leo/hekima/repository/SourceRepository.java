package com.leo.hekima.repository;

import com.leo.hekima.model.SourceModel;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SourceRepository extends ReactiveCrudRepository<SourceModel, Long> {
    Mono<SourceModel> findByUri(String source);

    Mono<Void> deleteByUri(String uri);
}
