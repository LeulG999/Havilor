package com.leul.havilor.common;

// Thrown by a service when the thing asked for does not exist. The global handler turns it into 404.
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + " " + id + " not found");
    }
}
