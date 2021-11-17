package com.leo.hekima.handler;

import com.leo.hekima.model.NoteQuizzHistoryModel;
import com.leo.hekima.model.NoteSummary;
import com.leo.hekima.repository.NoteQuizzHistoryRepository;
import com.leo.hekima.repository.NoteRepository;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.ElementSummaryView;
import com.leo.hekima.to.QuizzAnswerRequest;
import com.leo.hekima.utils.WebUtils;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Set;

import static com.leo.hekima.utils.ReactiveUtils.optionalEmptyDeferred;
import static com.leo.hekima.utils.RequestUtils.getCount;
import static com.leo.hekima.utils.RequestUtils.getStringSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

@Service
public class QuizzService {
    private final NoteQuizzHistoryRepository noteQuizzHistoryRepository;
    private final NoteRepository noteRepository;

    public QuizzService(NoteQuizzHistoryRepository noteQuizzHistoryRepository,
                        NoteRepository noteRepository) {
        this.noteQuizzHistoryRepository = noteQuizzHistoryRepository;
        this.noteRepository = noteRepository;
    }

    public record NoteAndHistory(NoteSummary note, NoteQuizzHistoryModel history){}

    public Mono<ServerResponse> generate(ServerRequest serverRequest) {
        final var now = Instant.now();
        final Set<String> tags = getStringSet(serverRequest, "tags");
        final Set<String> sources = getStringSet(serverRequest, "sources");
        final int count = getCount(serverRequest);
        final boolean emptyTags = isEmpty(tags);
        final boolean emptySources = isEmpty(sources);
        final Flux<NoteSummary> allNotes;
        if (emptySources) {
            allNotes = emptyTags ? noteRepository.findAllSummary() : noteRepository.findAllByTagsIn(tags);
        } else {
            allNotes = emptyTags ? noteRepository.findAllBySourceIn(sources) :
                    noteRepository.findAllBySourceInOrTagsIn(sources, tags);
        }
        final Flux<ElementSummaryView> notesAndHistory = allNotes.flatMap(n -> optionalEmptyDeferred(noteQuizzHistoryRepository.findLastByNoteId(n.noteid()))
                .map(histo -> new NoteAndHistory(n, histo.orElse(null))))
                // Sort by last asked date
                // If one of the note was never asked then it takes precedence
                // If both were never asked then the oldest one takes precedence
                .sort((nah1, nah2)  -> {
                    if(nah1.history == null) {
                        if(nah2.history == null) {
                            return nah2.note.createdat().compareTo(nah1.note.createdat());
                        } else {
                            return - 1;
                        }
                    } else if(nah2.history == null) {
                        return 1;
                    } else {
                        return  nah1.history.getCreatedAt().compareTo(nah2.history.getCreatedAt());
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
