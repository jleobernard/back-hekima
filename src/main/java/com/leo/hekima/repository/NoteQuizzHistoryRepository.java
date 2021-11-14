package com.leo.hekima.repository;


import com.leo.hekima.model.NoteQuizzHistoryModel;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface NoteQuizzHistoryRepository extends ReactiveCrudRepository<NoteQuizzHistoryModel, Long> {

    @Query("""
            SELECT nq.* FROM note_quizz_histo nq
            WHERE nq.note_id = :note_id
            ORDER BY created_at DESC
            LIMIT 1
            """)
    Mono<NoteQuizzHistoryModel> findLastByNoteId(@Param("note_id") Long noteId);
}
