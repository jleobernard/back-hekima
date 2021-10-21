package com.leo.hekima;

import com.leo.hekima.handler.NoteService;
import com.leo.hekima.handler.SourceService;
import com.leo.hekima.handler.TagService;
import com.leo.hekima.handler.UserService;
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
                                                final UserService userService) {
        return RouterFunctions
                .route(GET("/api/user")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        userService::me)
                .andRoute(GET("/api/authentication:status")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        userService::me)
                .andRoute(GET("/api/kosubs")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        subsService::search)
                .andRoute(GET("/api/notes")
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
                        noteService::getFile)
                .andRoute(GET("/api/sources")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        sourceService::search)
                .andRoute(POST("/api/sources")
                    .and(contentType(MediaType.APPLICATION_JSON))
                    .and(accept(MediaType.APPLICATION_JSON)),
                        sourceService::upsert)
                .andRoute(DELETE("/api/sources/{uri}")
                        .and(accept(MediaType.APPLICATION_JSON)),
                        sourceService::delete)
                .andRoute(GET("/api/tags")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        tagService::search)
                .andRoute(POST("/api/tags")
                    .and(contentType(MediaType.APPLICATION_JSON))
                    .and(accept(MediaType.APPLICATION_JSON)),
                    tagService::upsert)
                .andRoute(DELETE("/api/tags/{uri}")
                        .and(accept(MediaType.APPLICATION_JSON)),
                tagService::delete);
    }
}
