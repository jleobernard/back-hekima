package com.leo.hekima;

import com.leo.hekima.handler.HekimaHandler;
import com.leo.hekima.handler.SourceHandler;
import com.leo.hekima.handler.TagHandler;
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
    public RouterFunction<ServerResponse> route(HekimaHandler actionHandler,
                                                SourceHandler sourceHandler,
                                                TagHandler tagHandler) {
        return RouterFunctions
                .route(GET("/hekimas")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        actionHandler::search)
                .andRoute(GET("/sources")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        sourceHandler::search)
                .andRoute(GET("/tags")
                                .and(accept(MediaType.APPLICATION_JSON)),
                        tagHandler::search);
    }
}
