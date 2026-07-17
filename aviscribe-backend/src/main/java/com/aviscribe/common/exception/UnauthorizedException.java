package com.aviscribe.common.exception;

/**
 * Signals that the current request lacks valid authentication.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
