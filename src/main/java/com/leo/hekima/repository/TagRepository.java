package com.leo.hekima.repository;

import com.leo.hekima.model.TagModel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface TagRepository extends ReactiveCrudRepository<TagModel, Long> {

    Mono<TagModel> findByUri(final String uri);

    Flux<TagModel> findByUriIn(List<String> tags);

    @Query("""
            SELECT tag.*
            FROM tag
            LEFT JOIN note_tag ON note_tag.tag_id = tag.id
            WHERE note_tag.note_id = :noteId
            """)
    Flux<TagModel> findByNoteId(@Param("noteId") final Long noteId);

    Mono<Void> deleteByUri(String uri);
}
