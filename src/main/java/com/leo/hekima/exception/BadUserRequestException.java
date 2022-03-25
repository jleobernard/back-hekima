package com.leo.hekima.exception;

public class BadUserRequestException extends RuntimeException {
    public BadUserRequestException() {
    }

    public BadUserRequestException(String message) {
        super(message);
    }

    public BadUserRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadUserRequestException(Throwable cause) {
        super(cause);
    }

    public BadUserRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
