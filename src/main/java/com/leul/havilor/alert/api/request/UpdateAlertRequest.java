package com.leul.havilor.alert.api.request;

import com.leul.havilor.alert.domain.enums.Priority;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// PATCH /api/v1/alerts/{id}. Every field is optional: null (or left out) means "don't change it".
// For waitingOn and externalCaseNumber, an empty string clears the value.
// version (optional): the version the client last saw. If the alert has changed since, the update is refused (409).
public record UpdateAlertRequest(
        Priority priority,
        @Positive Long assigneeId,
        @Size(max = 200) String waitingOn,
        @Size(max = 50) String externalCaseNumber,
        LocalDate followUpOn,
        @PositiveOrZero Long version
) {
}
