package com.leul.havilor.common;

// Thrown by a service when a request is well-formed but breaks a business rule
// (e.g. changing a closed alert). The global handler turns it into 409.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
