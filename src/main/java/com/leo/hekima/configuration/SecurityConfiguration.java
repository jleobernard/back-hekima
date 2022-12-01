package com.leo.hekima.configuration;

import com.leo.hekima.service.JwtTokenProvider;
import com.leo.hekima.service.UserService;
import com.leo.hekima.web.JwtTokenAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10, new SecureRandom());
    }
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(final UserService userService,
                                                                       PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(userService);
        authenticationManager.setPasswordEncoder(passwordEncoder);
        return authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain securitygWebFilterChain(final ServerHttpSecurity http,
                                                          final JwtTokenProvider tokenProvider) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange()
            .pathMatchers("/api/login", "/api/version",
                    "/api/authentication:status",
                    "/api/kosubs:reload",
                    "/api/token:refresh",
                    "/api/notes/*/files/*"
                    /*,
                    "/api/kosubs"*/
            ).permitAll()
            .anyExchange().authenticated()
            .and()
            .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec.authenticationEntryPoint((exchange, ex) -> {
                logger.info("Received exception", ex);
                final String message = ex.getMessage().toLowerCase();
                if(message.contains("not authenticated")) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                } else {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                }
                return Mono.empty();
            }))
            .addFilterAt(new JwtTokenAuthenticationFilter(tokenProvider), SecurityWebFiltersOrder.HTTP_BASIC)
            .build();
    }
}
