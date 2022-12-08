package com.leo.hekima.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VersionService {
    public Mono<String> getVersion() {
        return Mono.just("20220516H1749");
    }
}
