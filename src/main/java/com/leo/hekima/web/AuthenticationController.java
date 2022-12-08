package com.leo.hekima.web;

import com.leo.hekima.service.AuthenticationService;
import com.leo.hekima.service.UserService;
import com.leo.hekima.to.AckResponse;
import com.leo.hekima.to.AuthenticationRequest;
import com.leo.hekima.to.AuthenticationResponse;
import com.leo.hekima.to.RefreshRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthenticationController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthenticationResponse>> authenticate(@RequestBody final AuthenticationRequest request) {
        return authenticationService.authenticate(request)
            .map(e -> ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + e.accessToken())
                .body(e))
            .onErrorResume(th -> {
                final ResponseEntity.BodyBuilder respEntity;
                if (th instanceof BadCredentialsException) {
                    respEntity = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
                } else {
                    respEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return Mono.just(respEntity.build());
            });
    }

    @PostMapping("/authentication:status")
    public Mono<ResponseEntity<AckResponse>> authenticationStatus() {
        return userService.me()
            .map(e -> ResponseEntity.ok().body(e))
            .onErrorReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/token:refresh")
    public Mono<ResponseEntity<AuthenticationResponse>> refreshToken(@RequestBody final RefreshRequest request) {
        return authenticationService.refresh(request)
            .map(e -> ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + e.accessToken())
                .body(e))
            .onErrorResume(th -> Mono.just(ResponseEntity.badRequest().build()));
    }
}
