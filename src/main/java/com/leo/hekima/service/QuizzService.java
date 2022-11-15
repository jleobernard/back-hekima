package com.leo.hekima.service;

import com.leo.hekima.model.NoteQuizzHistoryModel;
import com.leo.hekima.model.NoteSummary;
import com.leo.hekima.repository.NoteQuizzHistoryRepository;
import com.leo.hekima.repository.NoteRepository;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.ElementSummaryView;
import com.leo.hekima.to.QuizzAnswerRequest;
import com.leo.hekima.utils.WebUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.leo.hekima.utils.ReactiveUtils.optionalEmptyDeferred;
import static com.leo.hekima.utils.RequestUtils.getCount;
import static com.leo.hekima.utils.RequestUtils.getStringSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

@Service
public class QuizzService {
    private final NoteQuizzHistoryRepository noteQuizzHistoryRepository;
    private final NoteRepository noteRepository;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public QuizzService(NoteQuizzHistoryRepository noteQuizzHistoryRepository,
                        NoteRepository noteRepository, R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.noteQuizzHistoryRepository = noteQuizzHistoryRepository;
        this.noteRepository = noteRepository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    public record NoteAndHistory(NoteSummary note, NoteQuizzHistoryModel history){}

    public Mono<ServerResponse> generate(ServerRequest serverRequest) {
        final String baseRequest = """
        SELECT DISTINCT ON (n.id) n.id as noteid, n.uri as noteuri, n.created_at as createdat, n.valeur as valeur FROM note n
        LEFT JOIN note_tag nt ON n.id = nt.note_id
        LEFT JOIN tag t ON nt.tag_id = t.id
        LEFT JOIN note_source s ON n.source_id = s.id
        """;
        final Set<String> tags = getStringSet(serverRequest, "tags");
        final Set<String> notTags = getStringSet(serverRequest, "notTags");
        final Set<String> sources = getStringSet(serverRequest, "sources");
        final int count = getCount(serverRequest);
        final List<String> conditions = new ArrayList<>(3);
        if(isNotEmpty(tags)) {
            conditions.add(" t.uri IN ('" + String.join("','", tags) + "')");
        }
        if(isNotEmpty(notTags)) {
            conditions.add("""
                    n.id NOT IN (SELECT distinct(_nt.note_id) FROM note_tag _nt LEFT JOIN tag _t ON _nt.tag_id = _t.id
                                                        WHERE _t.uri IN ('""" + String.join("','", notTags) + "'))");
        }
        if(isNotEmpty(sources)) {
            conditions.add("s.uri IN ('" + String.join("','", sources) + "')");
        }
        final String request = baseRequest + (isEmpty(conditions) ? "" : " WHERE ") + String.join(" AND ", conditions) + " ORDER BY n.id";
        final Flux<NoteSummary> allNotes = r2dbcEntityTemplate.getDatabaseClient().sql(request)
            .map(row -> new NoteSummary(
                    row.get("noteid", Long.class),
                    row.get("noteuri", String.class),
                    row.get("createdat", Instant.class),
                    row.get("valeur", String.class)
            ))
            .all();
        final Flux<ElementSummaryView> notesAndHistory = allNotes.flatMap(n -> optionalEmptyDeferred(noteQuizzHistoryRepository.findLastByNoteId(n.noteid()))
                .map(histo -> new NoteAndHistory(n, histo.orElse(null))))
                // Sort by last asked date
                // If one of the note was never asked then it takes precedence
                // If both were never asked then the oldest one takes precedence
                .sort((nah1, nah2) -> {
                    if (nah1.history == null) {
                        if (nah2.history == null) {
                            return nah2.note.createdat().compareTo(nah1.note.createdat());
                        } else {
                            return -1;
                        }
                    } else if (nah2.history == null) {
                        return 1;
                    } else {
                        return nah1.history.getCreatedAt().compareTo(nah2.history.getCreatedAt());
                    }
                })
                .take(count)
                .map(n -> new ElementSummaryView(n.note.noteuri(), StringUtils.substring(n.note.valeur(), 0, 20)));
        return WebUtils.ok().body(notesAndHistory, ElementSummaryView.class);
    }

    public Mono<ServerResponse> answer(ServerRequest serverRequest) {
        return serverRequest.body(toMono(QuizzAnswerRequest.class))
            .flatMap(request ->  noteRepository.findByUri(request.noteUri())
                .flatMap(note -> noteQuizzHistoryRepository.save(new NoteQuizzHistoryModel(note.getId(), request.score()))))
            .flatMap(n -> WebUtils.ok().bodyValue(new AckResponse(true, "ok")));
    }
}
