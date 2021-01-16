package com.leo.hekima;

import com.leo.hekima.handler.HekimaService;
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
    public RouterFunction<ServerResponse> route(HekimaService hekimaService,
                                                SourceService sourceService,
                                                TagService tagService) {
        return RouterFunctions
                .route(GET("/api/hekimas")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        hekimaService::search)
                .andRoute(POST("/api/hekimas")
                                .and(contentType(MediaType.APPLICATION_JSON))
                                .and(accept(MediaType.APPLICATION_JSON)),
                        hekimaService::upsert)
                .andRoute(DELETE("/api/hekimas/{uri}")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        hekimaService::delete)
                .andRoute(GET("/api/hekimas/{uri}")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        hekimaService::findByUri)
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
