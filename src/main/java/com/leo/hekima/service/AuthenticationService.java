package com.leo.hekima.service;

import com.leo.hekima.model.RefreshTokenModel;
import com.leo.hekima.repository.RefreshTokenRepository;
import com.leo.hekima.repository.UserRepository;
import com.leo.hekima.to.AuthenticationRequest;
import com.leo.hekima.to.AuthenticationResponse;
import com.leo.hekima.to.RefreshRequest;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.leo.hekima.utils.WebUtils.ok;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;

@Service
public class AuthenticationService {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final ReactiveAuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public AuthenticationService(JwtTokenProvider jwtTokenProvider, ReactiveAuthenticationManager authenticationManager,
                                 RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Mono<ServerResponse> authenticate(ServerRequest serverRequest) {
        return serverRequest.body(toMono(AuthenticationRequest.class))
        .flatMap(request -> {
            logger.info("Authenticating user {}", request.username());
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()))
                    .map(authentication -> Pair.of(((User) authentication.getPrincipal()).getUsername(), jwtTokenProvider.createToken(authentication)))
                .flatMap(authAndJwt -> userRepository.findByUri(authAndJwt.getFirst()).map(user -> Pair.of(user, authAndJwt.getSecond())))
                .flatMap(userAndAccessToken -> refreshTokenRepository.save(
                        new RefreshTokenModel(userAndAccessToken.getFirst().getId(), UUID.randomUUID().toString()))
                    .map(rt -> Triple.of(userAndAccessToken.getFirst(), userAndAccessToken.getSecond(), rt)))
                .flatMap(userAccessAndRefresh -> ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessAndRefresh.getMiddle())
                    .bodyValue(new AuthenticationResponse(userAccessAndRefresh.getLeft().getUri(),
                        userAccessAndRefresh.getMiddle(),
                        userAccessAndRefresh.getRight().getToken()))
                );
            }
        ).onErrorResume(throwable -> {
            if (throwable instanceof BadCredentialsException) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        });
    }

    @Transactional
    public Mono<ServerResponse> refresh(ServerRequest serverRequest) {
        return serverRequest.body(toMono(RefreshRequest.class))
            .flatMap(token ->
                refreshTokenRepository.findUserByRefreshToken(token.getRefreshToken())
            .flatMap(user -> refreshTokenRepository.deleteByToken(token.getRefreshToken()).then(
                refreshTokenRepository.save(new RefreshTokenModel(user.getId(), UUID.randomUUID().toString())).map(
                    rt -> Triple.of(user, jwtTokenProvider.createToken(user), rt)
                )
            ))
            .flatMap(userAccessAndRefresh -> ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessAndRefresh.getMiddle())
                .bodyValue(new AuthenticationResponse(userAccessAndRefresh.getLeft().getUri(),
                        userAccessAndRefresh.getMiddle(),
                        userAccessAndRefresh.getRight().getToken()))
            ))
            .onErrorStop()
            .switchIfEmpty(Mono.defer(() -> ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).bodyValue("token_not_valid")));
    }
}
