package com.leul.havilor.common.error;

// Input that is well-formed but inconsistent in a way Bean Validation can't see on one field,
// e.g. a click that names a recipient not listed in the same request. The global handler turns it into 400.
public class BadRequestException extends RuntimeException {

    private final String field;

    public BadRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
