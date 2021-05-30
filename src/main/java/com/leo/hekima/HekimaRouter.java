package com.leo.hekima;

import com.leo.hekima.handler.NoteService;
import com.leo.hekima.handler.SourceService;
import com.leo.hekima.handler.TagService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class HekimaRouter {

    @Bean
    public RouterFunction<ServerResponse> route(NoteService noteService,
                                                SourceService sourceService,
                                                TagService tagService) {
        return RouterFunctions
                .route(GET("/api/notes")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::search)
                .andRoute(POST("/api/notes")
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
                .andRoute(POST("/api/notes/{uri}/file")
                            .and(contentType(MediaType.MULTIPART_FORM_DATA))
                            .and(accept(MediaType.APPLICATION_JSON)),
                        noteService::uploadFile)
                .andRoute(GET("/api/notes/{uri}/file"),
                        noteService::getFile)
                .andRoute(DELETE("/api/notes/{uri}/file"),
                        noteService::deleteFile)
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
