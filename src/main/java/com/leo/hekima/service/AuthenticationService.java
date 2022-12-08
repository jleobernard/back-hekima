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
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

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
    public Mono<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        logger.info("Authenticating user {}", request.username());
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()))
                .map(authentication -> Pair.of(((User) authentication.getPrincipal()).getUsername(), jwtTokenProvider.createToken(authentication)))
            .flatMap(authAndJwt -> userRepository.findByUri(authAndJwt.getFirst()).map(user -> Pair.of(user, authAndJwt.getSecond())))
            .flatMap(userAndAccessToken -> refreshTokenRepository.save(
                    new RefreshTokenModel(userAndAccessToken.getFirst().getId(), UUID.randomUUID().toString()))
                .map(rt -> Triple.of(userAndAccessToken.getFirst(), userAndAccessToken.getSecond(), rt)))
            .map(userAccessAndRefresh -> new AuthenticationResponse(userAccessAndRefresh.getLeft().getUri(),
                    userAccessAndRefresh.getMiddle(),
                    userAccessAndRefresh.getRight().getToken()));
    }

    @Transactional
    public Mono<AuthenticationResponse> refresh(final RefreshRequest request) {
        return refreshTokenRepository.findUserByRefreshToken(request.getRefreshToken())
            .flatMap(user -> refreshTokenRepository.deleteByToken(request.getRefreshToken()).then(
                refreshTokenRepository.save(new RefreshTokenModel(user.getId(), UUID.randomUUID().toString())).map(
                    rt -> Triple.of(user, jwtTokenProvider.createToken(user), rt)
                )
            ))
            .map(userAccessAndRefresh -> new AuthenticationResponse(userAccessAndRefresh.getLeft().getUri(),
                        userAccessAndRefresh.getMiddle(),
                        userAccessAndRefresh.getRight().getToken()))
            .onErrorStop();
    }
}
