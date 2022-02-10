package com.leo.hekima.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.leo.hekima.utils.WebUtils.ok;

@Service
public class VersionService {
    public Mono<ServerResponse> getVersion(ServerRequest serverRequest) {
        return ok().bodyValue("202202102133");
    }
}
