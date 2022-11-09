package com.leo.hekima.service;

import com.leo.hekima.repository.UserRepository;
import com.leo.hekima.to.AckResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.leo.hekima.utils.WebUtils.ok;

@Service
public class UserService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .map(userModel -> User.withUsername(userModel.getUri())
                        .password(userModel.getPassword())
                        .authorities("USER")
                        .accountExpired(false)
                        .credentialsExpired(false)
                        .disabled(false)
                        .accountLocked(false)
                        .build());
    }

    public static Mono<Authentication> getAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
            .mapNotNull(SecurityContext::getAuthentication)
            .filter(auth -> ! (auth instanceof AnonymousAuthenticationToken));
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        return getAuthentication()
            .flatMap(auth -> ok().bodyValue(AckResponse.OK))
            .switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
    }
}
