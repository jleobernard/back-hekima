package com.leo.hekima.web;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CacheFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        var path = serverWebExchange.getRequest().getPath().toString();
        if(path.matches("/api/notes/[0-9a-z]+/files/[0-9a-z]+")) {
            serverWebExchange.getResponse()
                    .getHeaders().add("Cache-Control", "max-age=31536000");
        }
        return webFilterChain.filter(serverWebExchange);

    }
}
