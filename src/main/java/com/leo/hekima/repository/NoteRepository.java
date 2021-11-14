package com.leo.hekima.repository;


import com.leo.hekima.model.NoteModel;
import com.leo.hekima.model.NoteSummary;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface NoteRepository extends ReactiveCrudRepository<NoteModel, Long> {
    Mono<NoteModel> findByUri(String uri);

    @Modifying
    @Query("""
            DELETE FROM note_tag WHERE note_id = :noteid
            """)
    Mono<Void> deleteLinkWithTags(@Param("noteid") Long id);

    @Query("""
            SELECT n.id as noteid, n.uri as noteuri, n.created_at as createdat, n.valeur as valeur FROM note n
            """)
    Flux<NoteSummary> findAllSummary();

    @Query("""
            SELECT DISTINCT ON (n.id) n.id as noteid, n.uri as noteuri, n.created_at as createdat, n.valeur as valeur FROM note n
            LEFT JOIN note_tag nt ON n.id = nt.note_id
            LEFT JOIN tag t ON nt.tag_id = t.id
            WHERE t.uri IN (:tags)
            ORDER BY n.id
            """)
    Flux<NoteSummary> findAllByTagsIn(@Param("tags") Set<String> tags);

    @Query("""
            SELECT n.id as noteid, n.uri as noteuri, n.created_at as createdat, n.valeur as valeur FROM note n
            LEFT JOIN note_source s ON n.source_id = s.id
            WHERE s.uri IN (:sources)
            """)
    Flux<NoteSummary> findAllBySourceIn(@Param("sources") Set<String> sources);

    @Query("""
            SELECT DISTINCT ON (n.id) n.id as noteid, n.uri as noteuri, n.created_at as createdat, n.valeur as valeur FROM note n
            LEFT JOIN note_source s ON n.source_id = s.id
            LEFT JOIN note_tag nt ON n.id = nt.note_id
            LEFT JOIN tag t ON nt.tag_id = t.id
            WHERE s.uri IN (:sources)
               OR t.uri IN (:tags)
            ORDER BY n.id
            """)
    Flux<NoteSummary> findAllBySourceInOrTagsIn(@Param("sources") Set<String> sources, @Param("tags") Set<String> tags);
}
