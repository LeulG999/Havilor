package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.enums.Verdict;
import jakarta.validation.constraints.NotNull;

// POST /api/v1/alerts/{id}/close. The analyst's final decision. UNDECIDED is refused by Alert.close (400).
public record CloseAlertRequest(
        @NotNull Verdict verdict
) {
}
