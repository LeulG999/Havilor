package com.leul.havilor.common.error;

import java.time.Instant;
import java.util.List;

// The one error body every endpoint returns. fieldErrors is empty unless the request had invalid fields.
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldViolation> fieldErrors
) {

    public record FieldViolation(String field, String message) {
    }
}
