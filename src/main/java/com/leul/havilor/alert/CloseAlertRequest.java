package com.leul.havilor.alert;

import jakarta.validation.constraints.NotNull;

// POST /api/v1/alerts/{id}/close. The analyst's final decision.
public record CloseAlertRequest(
        @NotNull @DecidedVerdict Verdict verdict
) {
}
