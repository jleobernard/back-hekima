package com.leo.hekima;

import com.leo.hekima.service.*;
import com.leo.hekima.subs.SubsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class NoteRouter {

    @Bean
    public RouterFunction<ServerResponse> route(final NoteService noteService,
                                                final SourceService sourceService,
                                                final TagService tagService,
                                                final SubsService subsService,
                                                final UserService userService,
                                                final QuizzService quizzService,
                                                final AuthenticationService authenticationService) {
        RouterFunction<ServerResponse> f = routeSubs(subsService);
        f = routeNotes(noteService, f);
        f = routeSources(sourceService, f);
        f = routeQuizz(quizzService, f);
        return f.andRoute(
                GET("/api/user")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        userService::me)
                .andRoute(POST("/api/login")
                    .and(contentType(MediaType.APPLICATION_JSON))
                    .and(accept(MediaType.APPLICATION_JSON)),
                        authenticationService::authenticate)
                .andRoute(GET("/api/authentication:status")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        userService::me)
                .andRoute(GET("/api/tags")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        tagService::search)
                .andRoute(POST("/api/token:refresh")
                                .and(accept(MediaType.APPLICATION_JSON))
                                .and(contentType(MediaType.APPLICATION_JSON)),
                        authenticationService::refresh)
                .andRoute(GET("/api/tags/{uri}")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        tagService::findByUri)
                .andRoute(POST("/api/tags")
                    .and(contentType(MediaType.APPLICATION_JSON))
                    .and(accept(MediaType.APPLICATION_JSON)),
                    tagService::upsert)
                .andRoute(DELETE("/api/tags/{uri}")
                        .and(accept(MediaType.APPLICATION_JSON)),
                tagService::delete);
    }

    private RouterFunction<ServerResponse> routeSubs(SubsService subsService) {
        return RouterFunctions
        .route(GET("/api/kosubs").and(accept(MediaType.APPLICATION_JSON)), subsService::search)
        .andRoute(GET("/api/kosubs:explain").and(accept(MediaType.APPLICATION_JSON)), subsService::explain)
        .andRoute(GET("/api/kosubs:reload").and(accept(MediaType.APPLICATION_JSON)), subsService::askReloadDb)
        .andRoute(GET("/api/kosubs:autocomplete").and(accept(MediaType.APPLICATION_JSON)), subsService::autocomplete)
        .andRoute(GET("/api/kosubs/{videoName}/texts").and(accept(MediaType.APPLICATION_JSON)), subsService::text);
    }
    private RouterFunction<ServerResponse> routeSources(SourceService sourceService, RouterFunction<ServerResponse> f) {
        return f.andRoute(GET("/api/sources")
                    .and(accept(MediaType.APPLICATION_JSON)),
            sourceService::search)
            .andRoute(GET("/api/sources/{uri}")
                            .and(accept(MediaType.APPLICATION_JSON)),
                    sourceService::findByUri)
            .andRoute(POST("/api/sources")
                            .and(contentType(MediaType.APPLICATION_JSON))
                            .and(accept(MediaType.APPLICATION_JSON)),
                    sourceService::upsert)
            .andRoute(DELETE("/api/sources/{uri}")
                            .and(accept(MediaType.APPLICATION_JSON)),
                    sourceService::delete);
    }
    private RouterFunction<ServerResponse> routeQuizz(QuizzService quizzService, RouterFunction<ServerResponse> f) {
        return f
            .andRoute(GET("/api/quizz:generate") .and(accept(MediaType.APPLICATION_JSON)), quizzService::generate)
            .andRoute(POST("/api/quizz:answer") .and(accept(MediaType.APPLICATION_JSON)), quizzService::answer);
    }
    private RouterFunction<ServerResponse> routeNotes(NoteService noteService, RouterFunction<ServerResponse> f) {
        return f.andRoute(GET("/api/notes")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::search)
                .andRoute(POST("/api/notes")
                                .and(contentType(MediaType.APPLICATION_JSON))
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::upsert)
                .andRoute(POST("/api/notes/{uri}")
                                .and(contentType(MediaType.APPLICATION_JSON))
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::upsert)
                .andRoute(GET("/api/notes:count").and(accept(MediaType.APPLICATION_JSON)),
                        noteService::count)
                .andRoute(GET("/api/notes:autocomplete-index").and(accept(MediaType.APPLICATION_JSON)),
                        noteService::autoCompleteIndex)
                .andRoute(POST("/api/notes:reindex").and(accept(MediaType.APPLICATION_JSON)),
                        noteService::reindex)
                .andRoute(POST("/api/notes:parse").and(contentType(MediaType.MULTIPART_FORM_DATA)).and(accept(MediaType.APPLICATION_JSON)),
                        noteService::parseNote)
                .andRoute(DELETE("/api/notes/{uri}")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::delete)
                .andRoute(GET("/api/notes/{uri}")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::findByUri)
                .andRoute(POST("/api/notes/{uri}/files")
                                .and(contentType(MediaType.MULTIPART_FORM_DATA))
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::patchFiles)
                .andRoute(GET("/api/notes/{uri}/files/{fileId}"),
                        noteService::getFile);
    }
}
