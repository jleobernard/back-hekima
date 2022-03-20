package com.leo.hekima.service;

import com.leo.hekima.to.AuthenticationRequest;
import com.leo.hekima.to.AuthenticationResponse;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.leo.hekima.utils.WebUtils.ok;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

@Service
public class AuthenticationService {
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveAuthenticationManager authenticationManager;

    public AuthenticationService(JwtTokenProvider jwtTokenProvider, ReactiveAuthenticationManager authenticationManager) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public Mono<ServerResponse> authenticate(ServerRequest serverRequest) {
        return serverRequest.body(toMono(AuthenticationRequest.class))
        .flatMap(request ->
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()))
                    .map(authentication -> Pair.of(authentication, jwtTokenProvider.createToken(authentication)))
        )
        .flatMap(authAndJwt -> ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authAndJwt.getSecond())
                .bodyValue(new AuthenticationResponse(((User) authAndJwt.getFirst().getPrincipal()).getUsername(), authAndJwt.getSecond()))
        );
    }
}
