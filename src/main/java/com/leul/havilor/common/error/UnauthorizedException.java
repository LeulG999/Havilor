package com.leul.havilor.common.error;

// Thrown when a login fails (unknown email, wrong password, inactive user). The global handler turns it into 401.
// The message never says which of the three it was.
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
