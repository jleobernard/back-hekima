package com.leo.hekima.exception;


import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class RestResponseStatusExceptionResolver {

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason="Bad credentials")
    @ExceptionHandler(value = BadCredentialsException.class)
    public void badCredentials() {}
}
