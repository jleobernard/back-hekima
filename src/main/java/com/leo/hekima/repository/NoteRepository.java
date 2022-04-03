package com.leo.hekima.repository;


import com.leo.hekima.model.NoteModel;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface NoteRepository extends ReactiveCrudRepository<NoteModel, Long> {
    Mono<NoteModel> findByUri(String uri);

    @Modifying
    @Query("""
            DELETE FROM note_tag WHERE note_id = :noteid
            """)
    Mono<Void> deleteLinkWithTags(@Param("noteid") Long id);

    @Modifying
    @Query("""
            DELETE FROM note_word WHERE note_id = :noteid
            """)
    Mono<Void> deleteLinkWithWords(@Param("noteid") Long id);
}
