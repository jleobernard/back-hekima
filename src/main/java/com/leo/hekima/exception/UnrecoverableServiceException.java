package com.leo.hekima.exception;

public class UnrecoverableServiceException extends RuntimeException {
    public UnrecoverableServiceException() {
    }

    public UnrecoverableServiceException(String message) {
        super(message);
    }

    public UnrecoverableServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnrecoverableServiceException(Throwable cause) {
        super(cause);
    }

    public UnrecoverableServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
